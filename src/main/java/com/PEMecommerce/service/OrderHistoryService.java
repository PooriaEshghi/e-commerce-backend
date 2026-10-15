package com.PEMecommerce.service;

import com.PEMecommerce.dao.OrderRepository;
import com.PEMecommerce.dao.ProductRepository;
import com.PEMecommerce.dto.OrderHistoryResponse;
import com.PEMecommerce.dto.OrderLineResponse;
import com.PEMecommerce.entity.Order;
import com.PEMecommerce.entity.OrderItem;
import com.PEMecommerce.entity.Product;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderHistoryService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderHistoryService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public List<OrderHistoryResponse> findForEmail(String email) {
        List<Order> orders = orderRepository.findByCustomer_EmailIgnoreCaseOrderByDateCreatedDesc(email);
        Map<Long, Product> products = loadProducts(orders);
        List<OrderHistoryResponse> history = new ArrayList<>();
        for (Order order : orders) {
            history.add(toResponse(order, products));
        }
        return history;
    }

    public OrderHistoryResponse findOne(String email, String trackingNumber) {
        Order order = orderRepository.findByOrderTrackingNumber(trackingNumber)
                .filter(found -> belongsTo(found, email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toResponse(order, loadProducts(List.of(order)));
    }

    private boolean belongsTo(Order order, String email) {
        return order.getCustomer() != null
                && order.getCustomer().getEmail() != null
                && order.getCustomer().getEmail().equalsIgnoreCase(email);
    }

    private Map<Long, Product> loadProducts(List<Order> orders) {
        List<Long> ids = new ArrayList<>();
        for (Order order : orders) {
            if (order.getOrderItems() == null) {
                continue;
            }
            for (OrderItem item : order.getOrderItems()) {
                ids.add(item.getProductId());
            }
        }
        Map<Long, Product> products = new HashMap<>();
        for (Product product : productRepository.findAllById(ids)) {
            products.put(product.getId(), product);
        }
        return products;
    }

    private OrderHistoryResponse toResponse(Order order, Map<Long, Product> products) {
        OrderHistoryResponse response = new OrderHistoryResponse();
        response.setOrderTrackingNumber(order.getOrderTrackingNumber());
        response.setTotalPrice(order.getTotalPrice());
        response.setTotalQuantity(order.getTotalQuantity());
        response.setStatus(order.getStatus());
        response.setDateCreated(order.getDateCreated());
        List<OrderItem> items = new ArrayList<>();
        if (order.getOrderItems() != null) {
            items.addAll(order.getOrderItems());
        }
        items.sort(Comparator.comparing(OrderItem::getId, Comparator.nullsLast(Long::compareTo)));
        for (OrderItem item : items) {
            Product product = products.get(item.getProductId());
            OrderLineResponse line = new OrderLineResponse();
            line.setProductId(item.getProductId());
            line.setName(product == null ? "Product " + item.getProductId() : product.getName());
            line.setImageUrl(item.getImageUrl());
            line.setQuantity(item.getQuantity());
            line.setUnitPrice(item.getUnitPrice());
            response.getItems().add(line);
        }
        return response;
    }
}
