package com.holodos.purchases.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.catalog.application.ProductResolver;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.common.application.DomainEventPublisher;
import com.holodos.common.application.events.OperationLogEvent;
import com.holodos.common.domain.ConflictException;
import com.holodos.inventory.domain.StockEntry;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {
    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock PurchaseEventRepository purchaseEventRepository;
    @Mock StockEntryRepository stockEntryRepository;
    @Mock StoragePlaceRepository storagePlaceRepository;
    @Mock DomainEventPublisher domainEventPublisher;
    @Mock ProductResolver productResolver;

    PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(shoppingListItemRepository, purchaseEventRepository, stockEntryRepository, storagePlaceRepository, domainEventPublisher, productResolver);
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
        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertEquals("PURCHASE_PROCESS", eventCaptor.getValue().eventType());
    }

    @Test
    void processPurchaseRejectsAlreadyCompletedItem() {
        Product product = new Product();
        ShoppingListItem item = new ShoppingListItem();
        item.setProduct(product);
        item.setStatus(ShoppingItemStatus.COMPLETED);
        when(shoppingListItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(ConflictException.class, () ->
            purchaseService.processPurchase(new ProcessPurchaseRequest(1L, BigDecimal.ONE, null, null, null, null)));
        verify(stockEntryRepository, never()).save(any());
        verify(purchaseEventRepository, never()).save(any());
    }

    @Test
    void processPurchaseLinksFreeTextItemToResolvedProduct() {
        StoragePlace place = new StoragePlace();
        ReflectionTestUtils.setField(place, "id", 5L);
        UnitOfMeasure unit = new UnitOfMeasure();
        ReflectionTestUtils.setField(unit, "id", 3L);
        Product product = new Product();
        product.setShelfLifeDays(7);
        product.setDefaultStoragePlace(place);
        product.setDefaultUnit(unit);

        ShoppingListItem item = new ShoppingListItem();
        item.setTitle("молоко");
        item.setUnit(unit);
        item.setStatus(ShoppingItemStatus.ACTIVE);
        item.setSource(ShoppingItemSource.KEEP_SYNC);

        OffsetDateTime purchasedAt = OffsetDateTime.now();
        when(shoppingListItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(storagePlaceRepository.findById(5L)).thenReturn(Optional.of(place));
        when(productResolver.resolveOrCreate("молоко", null, 3L, 5L, null)).thenReturn(product);
        when(purchaseEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        purchaseService.processPurchase(new ProcessPurchaseRequest(1L, BigDecimal.ONE, 5L, purchasedAt, null, null));

        assertSame(product, item.getProduct());
        ArgumentCaptor<StockEntry> stockCaptor = ArgumentCaptor.forClass(StockEntry.class);
        verify(stockEntryRepository).save(stockCaptor.capture());
        assertSame(product, stockCaptor.getValue().getProduct());
        assertEquals(purchasedAt.plusDays(7), stockCaptor.getValue().getExpiresAt());
    }
}
