package com.holodos.purchases.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.common.application.OperationLogService;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.purchases.api.PurchaseDtos.ProcessPurchaseRequest;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {
    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock PurchaseEventRepository purchaseEventRepository;
    @Mock StockEntryRepository stockEntryRepository;
    @Mock StoragePlaceRepository storagePlaceRepository;
    @Mock OperationLogService operationLogService;

    PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(shoppingListItemRepository, purchaseEventRepository, stockEntryRepository, storagePlaceRepository, operationLogService);
    }

    @Test
    void processPurchaseCreatesEventAndStockAndCompletesItem() {
        StoragePlace place = new StoragePlace();
        UnitOfMeasure unit = new UnitOfMeasure();
        Product product = new Product();
        product.setShelfLifeDays(7);
        product.setDefaultStoragePlace(place);
        product.setDefaultUnit(unit);

        ShoppingListItem item = new ShoppingListItem();
        item.setProduct(product);
        item.setUnit(unit);
        item.setStatus(ShoppingItemStatus.ACTIVE);
        item.setSource(ShoppingItemSource.MANUAL);

        OffsetDateTime purchasedAt = OffsetDateTime.now();
        when(shoppingListItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(purchaseEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        purchaseService.processPurchase(new ProcessPurchaseRequest(1L, BigDecimal.ONE, null, purchasedAt, null, "ok"));

        verify(purchaseEventRepository, times(1)).save(any());
        verify(stockEntryRepository, times(1)).save(any());
        verify(shoppingListItemRepository, times(1)).save(any());
        verify(operationLogService, times(1)).log(eq("PURCHASE_PROCESS"), eq("ShoppingListItem"), anyString(), anyMap());
    }
}
