package com.holodos.shopping.application;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.Store;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoreRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemResponse;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemUpsertRequest;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShoppingListService {
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;
    private final StoreRepository storeRepository;

    public ShoppingListService(ShoppingListItemRepository shoppingListItemRepository, ProductRepository productRepository,
                               UnitRepository unitRepository, StoreRepository storeRepository) {
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<ShoppingItemResponse> listActive() {
        return shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE).stream().map(this::map).toList();
    }

    public ShoppingItemResponse create(ShoppingItemUpsertRequest request) {
        ShoppingListItem item = new ShoppingListItem();
        apply(item, request);
        item.setStatus(ShoppingItemStatus.ACTIVE);
        return map(shoppingListItemRepository.save(item));
    }

    public ShoppingItemResponse update(Long id, ShoppingItemUpsertRequest request) {
        ShoppingListItem item = shoppingListItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Shopping item not found"));
        apply(item, request);
        return map(shoppingListItemRepository.save(item));
    }

    public void markCompleted(Long id) {
        ShoppingListItem item = shoppingListItemRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Shopping item not found"));
        item.setStatus(ShoppingItemStatus.COMPLETED);
        item.setCompletedAt(OffsetDateTime.now());
        shoppingListItemRepository.save(item);
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
        shoppingListItemRepository.save(item);
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
