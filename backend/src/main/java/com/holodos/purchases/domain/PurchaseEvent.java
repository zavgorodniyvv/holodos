package com.holodos.purchases.domain;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.Store;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.shopping.domain.ShoppingListItem;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "purchase_events")
public class PurchaseEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_item_id")
    private ShoppingListItem shoppingListItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private UnitOfMeasure unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "purchased_at", nullable = false)
    private OffsetDateTime purchasedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_place_id", nullable = false)
    private StoragePlace storagePlace;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }


    public Long getId() { return id; }
    public ShoppingListItem getShoppingListItem() { return shoppingListItem; }
    public Product getProduct() { return product; }
    public java.math.BigDecimal getQuantity() { return quantity; }
    public UnitOfMeasure getUnit() { return unit; }
    public Store getStore() { return store; }
    public OffsetDateTime getPurchasedAt() { return purchasedAt; }
    public StoragePlace getStoragePlace() { return storagePlace; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public String getComment() { return comment; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setShoppingListItem(ShoppingListItem shoppingListItem) { this.shoppingListItem = shoppingListItem; }
    public void setProduct(Product product) { this.product = product; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setUnit(UnitOfMeasure unit) { this.unit = unit; }
    public void setStore(Store store) { this.store = store; }
    public void setPurchasedAt(OffsetDateTime purchasedAt) { this.purchasedAt = purchasedAt; }
    public void setStoragePlace(StoragePlace storagePlace) { this.storagePlace = storagePlace; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setComment(String comment) { this.comment = comment; }
}
