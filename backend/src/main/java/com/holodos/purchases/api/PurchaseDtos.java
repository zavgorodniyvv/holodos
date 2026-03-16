package com.holodos.purchases.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class PurchaseDtos {
    private PurchaseDtos() {}

    public record ProcessPurchaseRequest(
        @NotNull Long shoppingListItemId,
        @NotNull @Positive BigDecimal actualQuantity,
        Long storagePlaceId,
        OffsetDateTime purchasedAt,
        OffsetDateTime expiresAt,
        String comment
    ) {}
}
