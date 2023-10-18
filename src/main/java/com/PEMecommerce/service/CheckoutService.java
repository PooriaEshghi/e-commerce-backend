package com.PEMecommerce.service;

import com.PEMecommerce.dto.Purchase;
import com.PEMecommerce.dto.PurchaseResponse;

public interface CheckoutService {
    PurchaseResponse placeOrder(Purchase purchase);
}
