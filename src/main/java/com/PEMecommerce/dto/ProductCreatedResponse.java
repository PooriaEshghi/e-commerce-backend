package com.PEMecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProductCreatedResponse {
    private Long id;
    private String sku;
    private String name;
    private BigDecimal unitPrice;
    private Long categoryId;
}
