package com.holodos.inventory.application;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.DomainEventPublisher;
import com.holodos.common.application.events.OperationLogEvent;
import com.holodos.inventory.api.InventoryDtos.AddStockRequest;
import com.holodos.inventory.api.InventoryDtos.AdjustStockRequest;
import com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest;
import com.holodos.inventory.api.InventoryDtos.MoveStockRequest;
import com.holodos.inventory.api.InventoryDtos.StockEntryResponse;
import com.holodos.inventory.domain.InventoryAdjustment;
import com.holodos.inventory.domain.InventoryMovement;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.InventoryAdjustmentRepository;
import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.shopping.application.ShoppingListService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {
    private final StockEntryRepository stockEntryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final ShoppingListService shoppingListService;
    private final DomainEventPublisher domainEventPublisher;
    private final InventoryAdjustmentRepository adjustmentRepository;

    public InventoryService(StockEntryRepository stockEntryRepository, InventoryMovementRepository movementRepository,
                            ProductRepository productRepository, UnitRepository unitRepository,
                            StoragePlaceRepository storagePlaceRepository, ShoppingListService shoppingListService,
                            DomainEventPublisher domainEventPublisher, InventoryAdjustmentRepository adjustmentRepository) {
        this.stockEntryRepository = stockEntryRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.storagePlaceRepository = storagePlaceRepository;
        this.shoppingListService = shoppingListService;
        this.domainEventPublisher = domainEventPublisher;
        this.adjustmentRepository = adjustmentRepository;
    }

    @Transactional(readOnly = true)
    public Page<StockEntryResponse> list(StockStatus status, Long storagePlaceId, String search, Pageable pageable) {
        Specification<StockEntry> spec = (root, q, cb) -> cb.conjunction();
        if (status != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        if (storagePlaceId != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("storagePlace").get("id"), storagePlaceId));
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("product").get("name")), pattern));
        }
        return stockEntryRepository.findAll(spec, pageable).map(this::map);
    }

    public StockEntryResponse addStock(AddStockRequest request) {
        Product product = productRepository.findById(request.productId()).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        UnitOfMeasure unit = unitRepository.findById(request.unitId()).orElseThrow(() -> new IllegalArgumentException("Unit not found"));
        StoragePlace storage = storagePlaceRepository.findById(request.storagePlaceId()).orElseThrow(() -> new IllegalArgumentException("Storage place not found"));

        StockEntry entry = new StockEntry();
        entry.setProduct(product);
        entry.setUnit(unit);
        entry.setStoragePlace(storage);
        entry.setQuantity(request.quantity());
        entry.setAddedAt(request.addedAt() == null ? OffsetDateTime.now() : request.addedAt());
        entry.setPurchasedAt(request.purchasedAt());
        entry.setExpiresAt(request.expiresAt());
        entry.setComment(request.comment());
        entry.setStatus(StockStatus.AVAILABLE);
        StockEntry saved = stockEntryRepository.save(entry);
        domainEventPublisher.publish(new OperationLogEvent("STOCK_ADD", "StockEntry", String.valueOf(saved.getId()),
            Map.of("productId", product.getId(), "quantity", saved.getQuantity())));
        return map(saved);
    }

    public StockEntryResponse consume(Long stockEntryId, ConsumeStockRequest request) {
        StockEntry entry = stockEntryRepository.findById(stockEntryId).orElseThrow(() -> new IllegalArgumentException("Stock entry not found"));
        if (entry.getStatus() != StockStatus.AVAILABLE) {
            throw new IllegalArgumentException("Stock entry is not available");
        }
        BigDecimal after = entry.getQuantity().subtract(request.quantity());
        entry.setQuantity(after.max(BigDecimal.ZERO));
        if (after.compareTo(BigDecimal.ZERO) <= 0) {
            entry.setStatus(StockStatus.CONSUMED);
            maybeRequestAutoReplenishment(entry, BigDecimal.ZERO);
        } else {
            maybeRequestAutoReplenishment(entry, after);
        }
        StockEntry saved = stockEntryRepository.save(entry);
        domainEventPublisher.publish(new OperationLogEvent("STOCK_CONSUME", "StockEntry", String.valueOf(saved.getId()),
            Map.of("consumed", request.quantity(), "remaining", saved.getQuantity())));
        return map(saved);
    }

    public StockEntryResponse discard(Long stockEntryId) {
        StockEntry entry = stockEntryRepository.findById(stockEntryId).orElseThrow(() -> new IllegalArgumentException("Stock entry not found"));
        entry.setStatus(StockStatus.DISCARDED);
        StockEntry saved = stockEntryRepository.save(entry);
        domainEventPublisher.publish(new OperationLogEvent("STOCK_DISCARD", "StockEntry", String.valueOf(saved.getId()),
            Map.of("status", saved.getStatus().name())));
        return map(saved);
    }

    public StockEntryResponse move(Long stockEntryId, MoveStockRequest request) {
        StockEntry entry = stockEntryRepository.findById(stockEntryId).orElseThrow(() -> new IllegalArgumentException("Stock entry not found"));
        if (entry.getStatus() != StockStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available stock can be moved");
        }
        StoragePlace target = storagePlaceRepository.findById(request.toStoragePlaceId())
            .orElseThrow(() -> new IllegalArgumentException("Target storage place not found"));
        if (entry.getStoragePlace().getId().equals(target.getId())) {
            throw new IllegalArgumentException("Source and destination storage places must differ");
        }
        if (!target.isActive()) {
            throw new IllegalArgumentException("Destination storage place must be active");
        }
        BigDecimal available = entry.getQuantity();
        if (available.compareTo(request.quantity()) < 0) {
            throw new IllegalArgumentException("Cannot move quantity larger than available stock");
        }

        StoragePlace source = entry.getStoragePlace();
        BigDecimal movingQty = request.quantity();
        if (movingQty.compareTo(available) < 0) {
            StockEntry remainder = cloneEntry(entry);
            remainder.setQuantity(available.subtract(movingQty));
            remainder.setStoragePlace(source);
            stockEntryRepository.save(remainder);
        }
        entry.setQuantity(movingQty);
        entry.setStoragePlace(target);
        StockEntry saved = stockEntryRepository.save(entry);

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(saved.getProduct());
        movement.setStockEntry(saved);
        movement.setFromStoragePlace(source);
        movement.setToStoragePlace(target);
        movement.setQuantity(movingQty);
        movement.setMovedAt(OffsetDateTime.now());
        movement.setComment(request.comment());
        movement.setUsername(request.username());
        movementRepository.save(movement);

        domainEventPublisher.publish(new OperationLogEvent("STOCK_MOVE", "StockEntry", String.valueOf(saved.getId()),
            Map.of("fromStoragePlaceId", source.getId(), "toStoragePlaceId", target.getId(), "quantity", movingQty)));
        return map(saved);
    }

    public StockEntryResponse adjust(Long stockEntryId, AdjustStockRequest request) {
        if (request.delta() == null || BigDecimal.ZERO.compareTo(request.delta()) == 0) {
            throw new IllegalArgumentException("Adjustment delta must be non zero");
        }
        StockEntry entry = stockEntryRepository.findById(stockEntryId).orElseThrow(() -> new IllegalArgumentException("Stock entry not found"));
        if (entry.getStatus() != StockStatus.AVAILABLE) {
            throw new IllegalArgumentException("Only available stock can be adjusted");
        }
        BigDecimal newQuantity = entry.getQuantity().add(request.delta());
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Adjustment would make quantity negative");
        }

        entry.setQuantity(newQuantity);
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            entry.setStatus(StockStatus.CONSUMED);
        } else {
            entry.setStatus(StockStatus.AVAILABLE);
        }

        StockEntry saved = stockEntryRepository.save(entry);

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setStockEntry(saved);
        adjustment.setProduct(saved.getProduct());
        adjustment.setDelta(request.delta());
        adjustment.setReason(request.reason());
        adjustment.setComment(request.comment());
        adjustment.setUsername(request.username());
        adjustment.setAdjustedAt(request.adjustedAt() == null ? OffsetDateTime.now() : request.adjustedAt());
        adjustmentRepository.save(adjustment);

        if (request.delta().signum() < 0) {
            maybeRequestAutoReplenishment(saved, newQuantity);
        }

        domainEventPublisher.publish(new OperationLogEvent("STOCK_ADJUST", "StockEntry", String.valueOf(saved.getId()),
            Map.of("delta", request.delta(), "reason", request.reason())));
        return map(saved);
    }

    private StockEntryResponse map(StockEntry e) {
        return new StockEntryResponse(e.getId(), e.getProduct().getId(), e.getQuantity(), e.getUnit().getId(), e.getStoragePlace().getId(),
            e.getAddedAt(), e.getPurchasedAt(), e.getExpiresAt(), e.getComment(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    private void maybeRequestAutoReplenishment(StockEntry entry, BigDecimal remainingQuantity) {
        Product product = entry.getProduct();
        if (!product.isAutoAddShopping()) {
            return;
        }
        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal threshold = product.getMinimumQuantityThreshold();
            if (threshold == null || remainingQuantity.compareTo(threshold) > 0) {
                return;
            }
        }
        BigDecimal replenishmentQty = product.getReorderQuantity() == null ? BigDecimal.ONE : product.getReorderQuantity();
        shoppingListService.autoAddIfMissing(product, replenishmentQty);
    }

    private StockEntry cloneEntry(StockEntry source) {
        StockEntry clone = new StockEntry();
        clone.setProduct(source.getProduct());
        clone.setUnit(source.getUnit());
        clone.setStoragePlace(source.getStoragePlace());
        clone.setQuantity(source.getQuantity());
        clone.setAddedAt(source.getAddedAt());
        clone.setPurchasedAt(source.getPurchasedAt());
        clone.setExpiresAt(source.getExpiresAt());
        clone.setOpenedAt(source.getOpenedAt());
        clone.setComment(source.getComment());
        clone.setStatus(source.getStatus());
        return clone;
    }
}
