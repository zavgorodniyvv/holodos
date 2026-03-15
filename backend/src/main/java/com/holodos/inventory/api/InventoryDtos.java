package com.holodos.inventory.api;

import com.holodos.inventory.domain.StockStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class InventoryDtos {
    private InventoryDtos() {}

    public record AddStockRequest(
        @NotNull Long productId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull Long unitId,
        @NotNull Long storagePlaceId,
        OffsetDateTime addedAt,
        OffsetDateTime purchasedAt,
        OffsetDateTime expiresAt,
        String comment
    ) {}

    public record ConsumeStockRequest(
        @NotNull @Positive BigDecimal quantity
    ) {}

    public record MoveStockRequest(
        @NotNull Long toStoragePlaceId,
        @NotNull @Positive BigDecimal quantity,
        String comment,
        @NotNull String username
    ) {}

    public record StockEntryResponse(
        Long id,
        Long productId,
        BigDecimal quantity,
        Long unitId,
        Long storagePlaceId,
        OffsetDateTime addedAt,
        OffsetDateTime purchasedAt,
        OffsetDateTime expiresAt,
        String comment,
        StockStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
    ) {}
}
