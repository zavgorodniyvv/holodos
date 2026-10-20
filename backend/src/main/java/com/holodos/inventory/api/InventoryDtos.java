package com.holodos.inventory.api;

import com.holodos.inventory.domain.StockStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class InventoryDtos {
    private InventoryDtos() {}

    /**
     * Either productId, or productName for a product that may not exist yet: it is then created with
     * unitId/storagePlaceId as its defaults plus the optional categoryId/shelfLifeDays (guessed when missing).
     */
    public record AddStockRequest(
        Long productId,
        @NotNull @Positive @Digits(integer = 11, fraction = 3) BigDecimal quantity,
        @NotNull Long unitId,
        @NotNull Long storagePlaceId,
        OffsetDateTime addedAt,
        OffsetDateTime purchasedAt,
        OffsetDateTime expiresAt,
        String comment,
        @Size(max = 200) String productName,
        Long categoryId,
        @Positive Integer shelfLifeDays
    ) {}

    public record ConsumeStockRequest(
        @NotNull @Positive @Digits(integer = 11, fraction = 3) BigDecimal quantity
    ) {}

    public record MoveStockRequest(
        @NotNull Long toStoragePlaceId,
        @NotNull @Positive @Digits(integer = 11, fraction = 3) BigDecimal quantity,
        String comment,
        @NotNull String username
    ) {}

    public record AdjustStockRequest(
        @NotNull @Digits(integer = 11, fraction = 3) BigDecimal delta,
        @NotBlank String reason,
        String comment,
        @NotNull String username,
        OffsetDateTime adjustedAt
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
