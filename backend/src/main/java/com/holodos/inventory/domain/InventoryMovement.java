package com.holodos.inventory.domain;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_entry_id", nullable = false)
    private StockEntry stockEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_storage_place_id", nullable = false)
    private StoragePlace fromStoragePlace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_storage_place_id", nullable = false)
    private StoragePlace toStoragePlace;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "moved_at", nullable = false)
    private OffsetDateTime movedAt;

    @Column
    private String comment;

    @Column(nullable = false)
    private String username;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }


    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public StockEntry getStockEntry() { return stockEntry; }
    public StoragePlace getFromStoragePlace() { return fromStoragePlace; }
    public StoragePlace getToStoragePlace() { return toStoragePlace; }
    public BigDecimal getQuantity() { return quantity; }
    public OffsetDateTime getMovedAt() { return movedAt; }
    public String getComment() { return comment; }
    public String getUsername() { return username; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setProduct(Product product) { this.product = product; }
    public void setStockEntry(StockEntry stockEntry) { this.stockEntry = stockEntry; }
    public void setFromStoragePlace(StoragePlace fromStoragePlace) { this.fromStoragePlace = fromStoragePlace; }
    public void setToStoragePlace(StoragePlace toStoragePlace) { this.toStoragePlace = toStoragePlace; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setMovedAt(OffsetDateTime movedAt) { this.movedAt = movedAt; }
    public void setComment(String comment) { this.comment = comment; }
    public void setUsername(String username) { this.username = username; }
}
