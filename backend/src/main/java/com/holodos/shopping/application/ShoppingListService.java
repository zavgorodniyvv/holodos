package com.holodos.shopping.application;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.Store;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoreRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.OperationLogService;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemResponse;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemUpsertRequest;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
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
public class ShoppingListService {
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;
    private final StoreRepository storeRepository;
    private final OperationLogService operationLogService;

    public ShoppingListService(ShoppingListItemRepository shoppingListItemRepository, ProductRepository productRepository,
                               UnitRepository unitRepository, StoreRepository storeRepository,
                               OperationLogService operationLogService) {
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.storeRepository = storeRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public Page<ShoppingItemResponse> list(ShoppingItemStatus status, Long storeId, String search, Pageable pageable) {
        Specification<ShoppingListItem> spec = Specification.where((Specification<ShoppingListItem>) null);
        if (status != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        if (storeId != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("store").get("id"), storeId));
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), pattern));
        }
        return shoppingListItemRepository.findAll(spec, pageable).map(this::map);
    }

    public ShoppingItemResponse create(ShoppingItemUpsertRequest request) {
        ShoppingListItem item = new ShoppingListItem();
        apply(item, request);
        item.setStatus(ShoppingItemStatus.ACTIVE);
        ShoppingListItem saved = shoppingListItemRepository.save(item);
        operationLogService.log("SHOPPING_CREATE", "ShoppingListItem", String.valueOf(saved.getId()), Map.of("title", saved.getTitle(), "source", saved.getSource().name()));
        return map(saved);
    }

    public ShoppingItemResponse update(Long id, ShoppingItemUpsertRequest request) {
        ShoppingListItem item = shoppingListItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Shopping item not found"));
        apply(item, request);
        ShoppingListItem saved = shoppingListItemRepository.save(item);
        operationLogService.log("SHOPPING_UPDATE", "ShoppingListItem", String.valueOf(saved.getId()), Map.of("title", saved.getTitle()));
        return map(saved);
    }

    public void markCompleted(Long id) {
        ShoppingListItem item = shoppingListItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Shopping item not found"));
        item.setStatus(ShoppingItemStatus.COMPLETED);
        item.setCompletedAt(OffsetDateTime.now());
        ShoppingListItem saved = shoppingListItemRepository.save(item);
        operationLogService.log("SHOPPING_COMPLETE", "ShoppingListItem", String.valueOf(saved.getId()), Map.of("completed", true));
    }

    public void autoAddIfMissing(Product product, BigDecimal quantity) {
        if (!product.isAutoAddShopping()) {
            return;
        }
        if (shoppingListItemRepository.findFirstByProductIdAndStatus(product.getId(), ShoppingItemStatus.ACTIVE).isPresent()) {
            return;
        }
        ShoppingListItem item = new ShoppingListItem();
        item.setProduct(product);
        item.setTitle(product.getName());
        item.setQuantity(quantity);
        item.setUnit(product.getDefaultUnit());
        item.setStore(product.getDefaultStore());
        item.setSource(ShoppingItemSource.AUTO_REPLENISHMENT);
        item.setStatus(ShoppingItemStatus.ACTIVE);
        item.setSortOrder(0);
        ShoppingListItem saved = shoppingListItemRepository.save(item);
        operationLogService.log("SHOPPING_AUTO_ADD", "ShoppingListItem", String.valueOf(saved.getId()), Map.of("productId", product.getId(), "quantity", quantity));
    }

    private void apply(ShoppingListItem item, ShoppingItemUpsertRequest request) {
        Product product = null;
        UnitOfMeasure unit = null;
        Store store = null;
        if (request.productId() != null) {
            product = productRepository.findById(request.productId()).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        }
        if (request.unitId() != null) {
            unit = unitRepository.findById(request.unitId()).orElseThrow(() -> new IllegalArgumentException("Unit not found"));
        }
        if (request.storeId() != null) {
            store = storeRepository.findById(request.storeId()).orElseThrow(() -> new IllegalArgumentException("Store not found"));
        }
        item.setProduct(product);
        item.setTitle(request.title());
        item.setQuantity(request.quantity());
        item.setUnit(unit);
        item.setStore(store);
        item.setSource(request.source());
        item.setComment(request.comment());
        item.setSortOrder(request.sortOrder());
    }

    private ShoppingItemResponse map(ShoppingListItem i) {
        return new ShoppingItemResponse(i.getId(), i.getProduct() == null ? null : i.getProduct().getId(), i.getTitle(), i.getQuantity(),
            i.getUnit() == null ? null : i.getUnit().getId(), i.getStore() == null ? null : i.getStore().getId(), i.getStatus(),
            i.getSource(), i.getComment(), i.getSortOrder(), i.getCreatedAt(), i.getCompletedAt(), i.getUpdatedAt(), i.getVersion());
    }
}
