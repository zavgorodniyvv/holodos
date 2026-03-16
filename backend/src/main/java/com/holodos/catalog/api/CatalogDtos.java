package com.holodos.catalog.api;

import com.holodos.catalog.domain.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record DictionaryUpsertRequest(
        @NotBlank String name,
        String description,
        String icon,
        String color,
        @NotNull @PositiveOrZero Integer sortOrder,
        @NotNull Boolean active
    ) {
    }

    public record UnitUpsertRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String shortName,
        @NotNull UnitType unitType,
        @NotNull Boolean active
    ) {
    }

    public record ProductUpsertRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        @NotNull Long defaultUnitId,
        @NotNull Long defaultStoragePlaceId,
        Long defaultStoreId,
        String photoKey,
        String description,
        Integer shelfLifeDays,
        BigDecimal minimumQuantityThreshold,
        BigDecimal reorderQuantity,
        @NotNull Boolean autoAddShopping,
        String barcode,
        String note,
        @NotNull Boolean active
    ) {
    }

    public record DictionaryResponse(
        Long id,
        String name,
        String description,
        String icon,
        String color,
        Integer sortOrder,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
    ) {
    }

    public record UnitResponse(
        Long id,
        String code,
        String name,
        String shortName,
        UnitType unitType,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
    ) {
    }

    public record ProductResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        Long defaultUnitId,
        String defaultUnitName,
        Long defaultStoragePlaceId,
        String defaultStoragePlaceName,
        Long defaultStoreId,
        String defaultStoreName,
        String photoKey,
        String description,
        Integer shelfLifeDays,
        BigDecimal minimumQuantityThreshold,
        BigDecimal reorderQuantity,
        Boolean autoAddShopping,
        String barcode,
        String note,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version
    ) {
    }
}
