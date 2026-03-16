package com.holodos.shopping.api;

import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class ShoppingDtos {
    private ShoppingDtos() {}

    public record ShoppingItemUpsertRequest(
        Long productId,
        @NotBlank String title,
        @NotNull @Positive BigDecimal quantity,
        Long unitId,
        Long storeId,
        @NotNull ShoppingItemSource source,
        String comment,
        @NotNull Integer sortOrder
    ) {}

    public record ShoppingItemResponse(
        Long id,
        Long productId,
        String title,
        BigDecimal quantity,
        Long unitId,
        Long storeId,
        ShoppingItemStatus status,
        ShoppingItemSource source,
        String comment,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt,
        OffsetDateTime updatedAt,
        Long version
    ) {}
}
