package com.holodos.inventory.domain;

import com.holodos.catalog.domain.Product;
import com.holodos.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_entry_id", nullable = false)
    private StockEntry stockEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal delta;

    @Column(nullable = false, length = 64)
    private String reason;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false, length = 120)
    private String username;

    @Column(name = "adjusted_at", nullable = false)
    private OffsetDateTime adjustedAt;

    public StockEntry getStockEntry() { return stockEntry; }
    public void setStockEntry(StockEntry stockEntry) { this.stockEntry = stockEntry; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public BigDecimal getDelta() { return delta; }
    public void setDelta(BigDecimal delta) { this.delta = delta; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public OffsetDateTime getAdjustedAt() { return adjustedAt; }
    public void setAdjustedAt(OffsetDateTime adjustedAt) { this.adjustedAt = adjustedAt; }
}
