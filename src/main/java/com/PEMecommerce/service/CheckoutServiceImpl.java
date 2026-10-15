package com.PEMecommerce.service;

import com.PEMecommerce.dao.CustomerRepository;
import com.PEMecommerce.dao.OrderRepository;
import com.PEMecommerce.dao.ProductRepository;
import com.PEMecommerce.dto.Purchase;
import com.PEMecommerce.dto.PurchaseResponse;
import com.PEMecommerce.entity.Address;
import com.PEMecommerce.entity.Customer;
import com.PEMecommerce.entity.Order;
import com.PEMecommerce.entity.OrderItem;
import com.PEMecommerce.entity.Product;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public CheckoutServiceImpl(CustomerRepository customerRepository,
                               OrderRepository orderRepository,
                               ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public PurchaseResponse placeOrder(Purchase purchase) {
        if (purchase == null || purchase.getOrderItems() == null || purchase.getOrderItems().isEmpty()) {
            throw new CheckoutException("The cart is empty");
        }
        Customer incoming = purchase.getCustomer();
        if (incoming == null) {
            throw new CheckoutException("Customer details are required");
        }
        String accountEmail = currentAccountEmail();
        String email = accountEmail != null
                ? accountEmail
                : normalizeEmail(incoming.getEmail());
        if (email == null || !email.contains("@")) {
            throw new CheckoutException("A valid email is required");
        }
        Address shippingAddress = requiredAddress(purchase.getShippingAddress(), "Shipping address is required");
        Address billingAddress = requiredAddress(purchase.getBillingAddress(), "Billing address is required");

        Map<Long, Integer> quantities = new HashMap<>();
        Map<Long, List<BigDecimal>> claimedPrices = new HashMap<>();
        for (OrderItem item : purchase.getOrderItems()) {
            if (item == null || item.getQuantity() <= 0) {
                throw new CheckoutException("Quantity must be at least 1");
            }
            if (item.getProductId() <= 0) {
                throw new CheckoutException("A product in the cart is not valid");
            }
            int already = quantities.getOrDefault(item.getProductId(), 0);
            if (item.getQuantity() > Integer.MAX_VALUE - already) {
                throw new CheckoutException("Quantity is too large");
            }
            quantities.put(item.getProductId(), already + item.getQuantity());
            claimedPrices.computeIfAbsent(item.getProductId(), id -> new ArrayList<>()).add(item.getUnitPrice());
        }

        Order order = new Order();
        order.setOrderTrackingNumber(UUID.randomUUID().toString());
        order.setStatus("PENDING");
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);

        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<Long> productIds = new ArrayList<>(quantities.keySet());
        productIds.sort(Long::compareTo);
        for (Long productId : productIds) {
            Product product = productRepository.findLockedById(productId)
                    .orElseThrow(() -> new CheckoutException("Product " + productId + " is not in the catalog"));
            if (!product.isActive()) {
                throw new CheckoutException(product.getName() + " is not available");
            }
            int requested = quantities.get(productId);
            if (product.getUnitsInStock() < requested) {
                throw new CheckoutException("Not enough stock for " + product.getName());
            }
            BigDecimal catalogPrice = product.getUnitPrice();
            if (catalogPrice == null) {
                throw new CheckoutException(product.getName() + " has no price");
            }
            for (BigDecimal claimed : claimedPrices.get(productId)) {
                if (claimed != null && !sameMoney(claimed, catalogPrice)) {
                    throw new CheckoutException("Price for " + product.getName() + " does not match the catalog");
                }
            }
            OrderItem line = new OrderItem();
            line.setProductId(product.getId());
            line.setQuantity(requested);
            line.setUnitPrice(money(catalogPrice));
            line.setImageUrl(product.getImageUrl());
            order.add(line);
            product.setUnitsInStock(product.getUnitsInStock() - requested);
            totalQuantity += requested;
            totalPrice = totalPrice.add(catalogPrice.multiply(BigDecimal.valueOf(requested)));
        }
        totalPrice = money(totalPrice);
        rejectClientTotal(purchase.getOrder(), totalPrice, totalQuantity);

        order.setTotalPrice(totalPrice);
        order.setTotalQuantity(BigDecimal.valueOf(totalQuantity));
        order.setCustomer(findOrCreateCustomer(email, incoming));
        orderRepository.save(order);
        return new PurchaseResponse(order.getOrderTrackingNumber());
    }

    private Customer findOrCreateCustomer(String email, Customer incoming) {
        return customerRepository.findFirstByEmailIgnoreCase(email).orElseGet(() -> {
            Customer created = new Customer();
            created.setEmail(email);
            created.setFirstName(trimToEmpty(incoming.getFirstName()));
            created.setLastName(trimToEmpty(incoming.getLastName()));
            return customerRepository.save(created);
        });
    }

    private void rejectClientTotal(Order incoming, BigDecimal totalPrice, int totalQuantity) {
        if (incoming == null) {
            return;
        }
        if (incoming.getTotalPrice() != null && !sameMoney(incoming.getTotalPrice(), totalPrice)) {
            throw new CheckoutException("Order total does not match the catalog prices");
        }
        if (incoming.getTotalQuantity() != null
                && incoming.getTotalQuantity().compareTo(BigDecimal.valueOf(totalQuantity)) != 0) {
            throw new CheckoutException("Order quantity does not match the cart");
        }
    }

    private Address requiredAddress(Address address, String message) {
        if (address == null || trimToEmpty(address.getStreet()).isEmpty()) {
            throw new CheckoutException(message);
        }
        address.setId(null);
        address.setOrder(null);
        return address;
    }

    private boolean sameMoney(BigDecimal claimed, BigDecimal catalog) {
        return money(claimed).compareTo(money(catalog)) == 0;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String currentAccountEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return normalizeEmail(authentication.getName());
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
