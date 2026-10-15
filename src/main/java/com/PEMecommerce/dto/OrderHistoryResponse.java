package com.PEMecommerce.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class OrderHistoryResponse {
    private String orderTrackingNumber;
    private BigDecimal totalPrice;
    private BigDecimal totalQuantity;
    private String status;
    private Date dateCreated;
    private List<OrderLineResponse> items = new ArrayList<>();
}
