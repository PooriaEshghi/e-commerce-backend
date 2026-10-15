package com.PEMecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    private String sku;
    private String name;
    private String description;
    private BigDecimal unitPrice;
    private String imageUrl;
    private Boolean active;
    private Integer unitsInStock;
    private Long categoryId;
}
