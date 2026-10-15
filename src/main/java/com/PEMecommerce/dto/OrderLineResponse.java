package com.PEMecommerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderLineResponse {
    private long productId;
    private String name;
    private String imageUrl;
    private int quantity;
    private BigDecimal unitPrice;
}
