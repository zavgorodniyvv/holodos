package com.holodos.catalog.api;

import com.holodos.catalog.domain.model.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CatalogDtos {

    public record StoragePlaceRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @Size(max = 64) String icon,
            @Size(max = 32) String color,
            int sortOrder,
            boolean active
    ) {}

    public record StoragePlaceResponse(
            UUID id, String name, String description, String icon, String color,
            int sortOrder, boolean active, long version, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    public record UnitRequest(
            @NotBlank @Size(max = 32) String code,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 20) String shortName,
            @NotNull UnitType unitType,
            boolean active
    ) {}

    public record UnitResponse(
            UUID id, String code, String name, String shortName, UnitType unitType,
            boolean active, long version, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    public record CategoryRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @Size(max = 64) String icon,
            @Size(max = 32) String color,
            int sortOrder,
            boolean active
    ) {}

    public record CategoryResponse(
            UUID id, String name, String description, String icon, String color,
            int sortOrder, boolean active, long version, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    public record StoreRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            @Size(max = 64) String icon,
            @Size(max = 32) String color,
            int sortOrder,
            boolean active
    ) {}

    public record StoreResponse(
            UUID id, String name, String description, String icon, String color,
            int sortOrder, boolean active, long version, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}

    public record ProductRequest(
            @NotBlank @Size(max = 160) String name,
            @NotNull UUID categoryId,
            @NotNull UUID defaultUnitId,
            @NotNull UUID defaultStoragePlaceId,
            UUID defaultStoreId,
            @Size(max = 500) String photoUrl,
            @Size(max = 1000) String description,
            Integer shelfLifeDays,
            BigDecimal minimumQuantityThreshold,
            BigDecimal reorderQuantity,
            boolean autoAddToShoppingList,
            @Size(max = 64) String barcode,
            @Size(max = 1000) String note,
            boolean active
    ) {}

    public record ProductResponse(
            UUID id, String name, UUID categoryId, UUID defaultUnitId, UUID defaultStoragePlaceId, UUID defaultStoreId,
            String photoUrl, String description, Integer shelfLifeDays,
            BigDecimal minimumQuantityThreshold, BigDecimal reorderQuantity,
            boolean autoAddToShoppingList, String barcode, String note,
            boolean active, long version, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {}
}
