package com.PEMecommerce.service;

import com.PEMecommerce.dao.CustomerRepository;
import com.PEMecommerce.dto.Purchase;
import com.PEMecommerce.dto.PurchaseResponse;
import com.PEMecommerce.entity.Customer;
import com.PEMecommerce.entity.Order;
import com.PEMecommerce.entity.OrderItem;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class CheckoutServiceImpl implements CheckoutService {
    private CustomerRepository customerRepository;
    public CheckoutServiceImpl(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }
    @Override
    @Transactional
    public PurchaseResponse placeOrder(Purchase purchase){
        Order order = purchase.getOrder();
        String orderTrackingNumber = generateOrderTrackingNumber();
        order.setOrderTrackingNumber(orderTrackingNumber);
        //______________________
        Set<OrderItem> orderItems = purchase.getOrderItems();
        orderItems.forEach(item -> order.add(item));
        //_______________________
        order.setBillingAddress(purchase.getBillingAddress());
        order.setShippingAddress(purchase.getShippingAddress());
        //_______________________
        Customer customer = purchase.getCustomer();
        customer.add(order);
        //_______________________
        customerRepository.save(customer);
        //________________________
        return new  PurchaseResponse(orderTrackingNumber) ;
    }

    private String generateOrderTrackingNumber() {
        return UUID.randomUUID().toString();
    }
}
