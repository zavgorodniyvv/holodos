package com.holodos.inventory.application;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.OperationLogService;
import com.holodos.inventory.api.InventoryDtos.AddStockRequest;
import com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest;
import com.holodos.inventory.api.InventoryDtos.MoveStockRequest;
import com.holodos.inventory.api.InventoryDtos.StockEntryResponse;
import com.holodos.inventory.domain.InventoryMovement;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.shopping.application.ShoppingListService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
    private final OperationLogService operationLogService;

    public InventoryService(StockEntryRepository stockEntryRepository, InventoryMovementRepository movementRepository,
                            ProductRepository productRepository, UnitRepository unitRepository,
                            StoragePlaceRepository storagePlaceRepository, ShoppingListService shoppingListService,
                            OperationLogService operationLogService) {
        this.stockEntryRepository = stockEntryRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.storagePlaceRepository = storagePlaceRepository;
        this.shoppingListService = shoppingListService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public Page<StockEntryResponse> list(StockStatus status, Long storagePlaceId, String search, Pageable pageable) {
        Specification<StockEntry> spec = Specification.where((Specification<StockEntry>) null);
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
        operationLogService.log("STOCK_ADD", "StockEntry", String.valueOf(saved.getId()), Map.of("productId", product.getId(), "quantity", saved.getQuantity()));
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
            BigDecimal replenishmentQty = entry.getProduct().getReorderQuantity() == null ? BigDecimal.ONE : entry.getProduct().getReorderQuantity();
            shoppingListService.autoAddIfMissing(entry.getProduct(), replenishmentQty);
        } else if (entry.getProduct().getMinimumQuantityThreshold() != null
            && after.compareTo(entry.getProduct().getMinimumQuantityThreshold()) <= 0) {
            BigDecimal replenishmentQty = entry.getProduct().getReorderQuantity() == null ? BigDecimal.ONE : entry.getProduct().getReorderQuantity();
            shoppingListService.autoAddIfMissing(entry.getProduct(), replenishmentQty);
        }
        StockEntry saved = stockEntryRepository.save(entry);
        operationLogService.log("STOCK_CONSUME", "StockEntry", String.valueOf(saved.getId()), Map.of("consumed", request.quantity(), "remaining", saved.getQuantity()));
        return map(saved);
    }

    public StockEntryResponse discard(Long stockEntryId) {
        StockEntry entry = stockEntryRepository.findById(stockEntryId).orElseThrow(() -> new IllegalArgumentException("Stock entry not found"));
        entry.setStatus(StockStatus.DISCARDED);
        StockEntry saved = stockEntryRepository.save(entry);
        operationLogService.log("STOCK_DISCARD", "StockEntry", String.valueOf(saved.getId()), Map.of("status", saved.getStatus().name()));
        return map(saved);
    }

    public StockEntryResponse move(Long stockEntryId, MoveStockRequest request) {
        StockEntry entry = stockEntryRepository.findById(stockEntryId).orElseThrow(() -> new IllegalArgumentException("Stock entry not found"));
        StoragePlace target = storagePlaceRepository.findById(request.toStoragePlaceId())
            .orElseThrow(() -> new IllegalArgumentException("Target storage place not found"));
        if (entry.getStoragePlace().getId().equals(target.getId())) {
            throw new IllegalArgumentException("Source and destination storage places must differ");
        }
        if (!target.isActive()) {
            throw new IllegalArgumentException("Destination storage place must be active");
        }
        if (entry.getQuantity().compareTo(request.quantity()) < 0) {
            throw new IllegalArgumentException("Cannot move quantity larger than available stock");
        }

        InventoryMovement movement = new InventoryMovement();
        movement.setProduct(entry.getProduct());
        movement.setStockEntry(entry);
        movement.setFromStoragePlace(entry.getStoragePlace());
        movement.setToStoragePlace(target);
        movement.setQuantity(request.quantity());
        movement.setMovedAt(OffsetDateTime.now());
        movement.setComment(request.comment());
        movement.setUsername(request.username());
        movementRepository.save(movement);

        entry.setStoragePlace(target);
        StockEntry saved = stockEntryRepository.save(entry);
        operationLogService.log("STOCK_MOVE", "StockEntry", String.valueOf(saved.getId()), Map.of("toStoragePlaceId", target.getId(), "quantity", request.quantity()));
        return map(saved);
    }

    private StockEntryResponse map(StockEntry e) {
        return new StockEntryResponse(e.getId(), e.getProduct().getId(), e.getQuantity(), e.getUnit().getId(), e.getStoragePlace().getId(),
            e.getAddedAt(), e.getPurchasedAt(), e.getExpiresAt(), e.getComment(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }
}
