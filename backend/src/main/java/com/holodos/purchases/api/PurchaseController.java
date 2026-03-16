package com.holodos.purchases.api;

import com.holodos.purchases.api.PurchaseDtos.ProcessPurchaseRequest;
import com.holodos.purchases.application.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/process")
    public void process(@Valid @RequestBody ProcessPurchaseRequest request) {
        purchaseService.processPurchase(request);
    }
}
