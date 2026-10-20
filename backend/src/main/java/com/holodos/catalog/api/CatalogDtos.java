package com.holodos.catalog.api;

import com.holodos.catalog.domain.UnitType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
        /** Ignored: the photo is managed via /api/products/{id}/photo. Kept for request compatibility. */
        String photoKey,
        String description,
        @Positive Integer shelfLifeDays,
        @PositiveOrZero @Digits(integer = 11, fraction = 3) BigDecimal minimumQuantityThreshold,
        @Positive @Digits(integer = 11, fraction = 3) BigDecimal reorderQuantity,
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

    public record ProductSuggestRequest(@NotBlank @Size(max = 200) String name) {
    }

    /** existingProductId is set when a product with this name already exists; the other fields are its defaults. */
    public record ProductSuggestionResponse(
        Long existingProductId,
        String name,
        Long categoryId,
        Long unitId,
        Long storagePlaceId,
        Integer shelfLifeDays,
        boolean aiUsed
    ) {
    }
}
