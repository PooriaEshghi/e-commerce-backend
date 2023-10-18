package com.PEMecommerce.dto;

import com.PEMecommerce.entity.Address;
import com.PEMecommerce.entity.Customer;
import com.PEMecommerce.entity.Order;
import com.PEMecommerce.entity.OrderItem;
import lombok.Data;

import java.util.Set;

@Data
public class Purchase {

    private Customer customer;
    private Address shippingAddress;
    private Address billingAddress;
    private Order order;
    private Set<OrderItem> orderItems;
}
