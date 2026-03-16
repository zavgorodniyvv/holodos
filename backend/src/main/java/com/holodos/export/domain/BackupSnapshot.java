package com.holodos.export.domain;

import com.holodos.catalog.domain.UnitType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record BackupSnapshot(
    OffsetDateTime exportedAt,
    List<StoragePlaceItem> storagePlaces,
    List<UnitItem> units,
    List<CategoryItem> categories,
    List<StoreItem> stores,
    List<ProductItem> products,
    List<ShoppingItem> shoppingItems
) {
    public record StoragePlaceItem(String name, String description, String icon, String color, Integer sortOrder, Boolean active) {}
    public record UnitItem(String code, String name, String shortName, UnitType unitType, Boolean active) {}
    public record CategoryItem(String name, String description, String icon, String color, Integer sortOrder, Boolean active) {}
    public record StoreItem(String name, String description, String icon, String color, Integer sortOrder, Boolean active) {}

    public record ProductItem(
        String name,
        String categoryName,
        String defaultUnitCode,
        String defaultStoragePlaceName,
        String defaultStoreName,
        String photoKey,
        String description,
        Integer shelfLifeDays,
        BigDecimal minimumQuantityThreshold,
        BigDecimal reorderQuantity,
        Boolean autoAddShopping,
        String barcode,
        String note,
        Boolean active
    ) {}

    public record ShoppingItem(
        String title,
        BigDecimal quantity,
        String unitCode,
        String storeName,
        String status,
        String source,
        String comment,
        Integer sortOrder
    ) {}
}
