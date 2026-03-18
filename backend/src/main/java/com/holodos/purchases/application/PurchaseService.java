package com.holodos.purchases.application;

import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.common.application.DomainEventPublisher;
import com.holodos.common.application.events.OperationLogEvent;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.purchases.api.PurchaseDtos.ProcessPurchaseRequest;
import com.holodos.purchases.domain.PurchaseEvent;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PurchaseService {
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final StockEntryRepository stockEntryRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final DomainEventPublisher domainEventPublisher;

    public PurchaseService(ShoppingListItemRepository shoppingListItemRepository, PurchaseEventRepository purchaseEventRepository,
                           StockEntryRepository stockEntryRepository, StoragePlaceRepository storagePlaceRepository,
                           DomainEventPublisher domainEventPublisher) {
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.storagePlaceRepository = storagePlaceRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    public void processPurchase(ProcessPurchaseRequest request) {
        ShoppingListItem item = shoppingListItemRepository.findById(request.shoppingListItemId())
            .orElseThrow(() -> new IllegalArgumentException("Shopping item not found"));
        if (item.getProduct() == null) {
            throw new IllegalArgumentException("Shopping item must be linked to a product");
        }

        OffsetDateTime purchasedAt = request.purchasedAt() == null ? OffsetDateTime.now() : request.purchasedAt();
        StoragePlace storagePlace = resolveStorage(item, request.storagePlaceId());
        OffsetDateTime expiresAt = request.expiresAt();
        if (expiresAt == null && item.getProduct().getShelfLifeDays() != null) {
            expiresAt = purchasedAt.plusDays(item.getProduct().getShelfLifeDays());
        }

        PurchaseEvent event = new PurchaseEvent();
        event.setShoppingListItem(item);
        event.setProduct(item.getProduct());
        event.setQuantity(request.actualQuantity());
        event.setUnit(item.getUnit() == null ? item.getProduct().getDefaultUnit() : item.getUnit());
        event.setStore(item.getStore());
        event.setPurchasedAt(purchasedAt);
        event.setStoragePlace(storagePlace);
        event.setExpiresAt(expiresAt);
        event.setComment(request.comment());
        purchaseEventRepository.save(event);

        StockEntry stockEntry = new StockEntry();
        stockEntry.setProduct(item.getProduct());
        stockEntry.setQuantity(request.actualQuantity());
        stockEntry.setUnit(item.getUnit() == null ? item.getProduct().getDefaultUnit() : item.getUnit());
        stockEntry.setStoragePlace(storagePlace);
        stockEntry.setAddedAt(purchasedAt);
        stockEntry.setPurchasedAt(purchasedAt);
        stockEntry.setExpiresAt(expiresAt);
        stockEntry.setComment(request.comment());
        stockEntry.setStatus(StockStatus.AVAILABLE);
        stockEntryRepository.save(stockEntry);

        item.setStatus(ShoppingItemStatus.COMPLETED);
        item.setCompletedAt(OffsetDateTime.now());
        shoppingListItemRepository.save(item);

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", item.getProduct().getId());
        payload.put("quantity", request.actualQuantity());
        domainEventPublisher.publish(new OperationLogEvent("PURCHASE_PROCESS", "ShoppingListItem", String.valueOf(item.getId()), payload));
    }

    private StoragePlace resolveStorage(ShoppingListItem item, Long storagePlaceId) {
        if (storagePlaceId != null) {
            return storagePlaceRepository.findById(storagePlaceId)
                .orElseThrow(() -> new IllegalArgumentException("Storage place not found"));
        }
        return item.getProduct().getDefaultStoragePlace();
    }
}
