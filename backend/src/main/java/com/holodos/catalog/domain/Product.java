package com.holodos.catalog.domain;

import com.holodos.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_unit_id", nullable = false)
    private UnitOfMeasure defaultUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_storage_place_id", nullable = false)
    private StoragePlace defaultStoragePlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_store_id")
    private Store defaultStore;

    @Column(name = "photo_key")
    private String photoKey;

    @Column
    private String description;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(name = "minimum_quantity_threshold", precision = 14, scale = 3)
    private BigDecimal minimumQuantityThreshold;

    @Column(name = "reorder_quantity", precision = 14, scale = 3)
    private BigDecimal reorderQuantity;

    @Column(name = "auto_add_shopping", nullable = false)
    private boolean autoAddShopping = true;

    @Column
    private String barcode;

    @Column(name = "note_text")
    private String note;

    @Column(nullable = false)
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public UnitOfMeasure getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(UnitOfMeasure defaultUnit) { this.defaultUnit = defaultUnit; }
    public StoragePlace getDefaultStoragePlace() { return defaultStoragePlace; }
    public void setDefaultStoragePlace(StoragePlace defaultStoragePlace) { this.defaultStoragePlace = defaultStoragePlace; }
    public Store getDefaultStore() { return defaultStore; }
    public void setDefaultStore(Store defaultStore) { this.defaultStore = defaultStore; }
    public String getPhotoKey() { return photoKey; }
    public void setPhotoKey(String photoKey) { this.photoKey = photoKey; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(Integer shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }
    public BigDecimal getMinimumQuantityThreshold() { return minimumQuantityThreshold; }
    public void setMinimumQuantityThreshold(BigDecimal minimumQuantityThreshold) { this.minimumQuantityThreshold = minimumQuantityThreshold; }
    public BigDecimal getReorderQuantity() { return reorderQuantity; }
    public void setReorderQuantity(BigDecimal reorderQuantity) { this.reorderQuantity = reorderQuantity; }
    public boolean isAutoAddShopping() { return autoAddShopping; }
    public void setAutoAddShopping(boolean autoAddShopping) { this.autoAddShopping = autoAddShopping; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
