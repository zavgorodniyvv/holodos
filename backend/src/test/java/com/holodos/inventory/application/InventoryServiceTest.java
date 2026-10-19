package com.holodos.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.DomainEventPublisher;
import com.holodos.common.application.events.OperationLogEvent;
import com.holodos.inventory.api.InventoryDtos.AdjustStockRequest;
import com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest;
import com.holodos.inventory.api.InventoryDtos.MoveStockRequest;
import com.holodos.inventory.domain.InventoryAdjustment;
import com.holodos.inventory.domain.InventoryMovement;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.InventoryAdjustmentRepository;
import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.notifications.application.NotificationService;
import com.holodos.shopping.application.ShoppingListService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock StockEntryRepository stockEntryRepository;
    @Mock InventoryMovementRepository movementRepository;
    @Mock InventoryAdjustmentRepository adjustmentRepository;
    @Mock ProductRepository productRepository;
    @Mock UnitRepository unitRepository;
    @Mock StoragePlaceRepository storagePlaceRepository;
    @Mock ShoppingListService shoppingListService;
    @Mock DomainEventPublisher domainEventPublisher;
    @Mock NotificationService notificationService;

    InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(stockEntryRepository, movementRepository, productRepository, unitRepository,
            storagePlaceRepository, shoppingListService, domainEventPublisher, adjustmentRepository, notificationService);
    }

    @Test
    void consumeToZeroTriggersAutoAdd() {
        Product product = new Product();
        product.setAutoAddShopping(true);
        product.setReorderQuantity(BigDecimal.valueOf(2));

        UnitOfMeasure unit = new UnitOfMeasure();
        StoragePlace sp = new StoragePlace();
        StockEntry stockEntry = new StockEntry();
        stockEntry.setProduct(product);
        stockEntry.setUnit(unit);
        stockEntry.setStoragePlace(sp);
        stockEntry.setQuantity(BigDecimal.ONE);
        stockEntry.setStatus(StockStatus.AVAILABLE);

        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(stockEntry));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = inventoryService.consume(10L, new ConsumeStockRequest(BigDecimal.ONE));

        assertEquals(StockStatus.CONSUMED, response.status());
        verify(shoppingListService).autoAddIfMissing(product, BigDecimal.valueOf(2));
        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        OperationLogEvent event = eventCaptor.getValue();
        assertEquals("STOCK_CONSUME", event.eventType());
    }

    @Test
    void moveSplitsEntryWhenMovingPartialQuantity() {
        Product product = new Product();
        product.setName("Milk");
        UnitOfMeasure unit = new UnitOfMeasure();
        StoragePlace source = new StoragePlace();
        StoragePlace target = new StoragePlace();
        ReflectionTestUtils.setField(source, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        StockEntry entry = new StockEntry();
        entry.setProduct(product);
        entry.setUnit(unit);
        entry.setStoragePlace(source);
        entry.setStatus(StockStatus.AVAILABLE);
        entry.setQuantity(BigDecimal.valueOf(5));

        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
        when(storagePlaceRepository.findById(2L)).thenReturn(Optional.of(target));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = inventoryService.move(10L, new MoveStockRequest(2L, BigDecimal.valueOf(2), "move", "tester"));

        assertEquals(BigDecimal.valueOf(2), response.quantity());
        assertEquals(2L, response.storagePlaceId());
        ArgumentCaptor<StockEntry> captor = ArgumentCaptor.forClass(StockEntry.class);
        verify(stockEntryRepository, times(2)).save(captor.capture());
        assertEquals(BigDecimal.valueOf(3), captor.getAllValues().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(2), captor.getAllValues().get(1).getQuantity());
        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movementCaptor.capture());
        assertEquals(BigDecimal.valueOf(2), movementCaptor.getValue().getQuantity());
        assertEquals(1L, movementCaptor.getValue().getFromStoragePlace().getId());
        assertEquals(2L, movementCaptor.getValue().getToStoragePlace().getId());
    }

    @Test
    void adjustDecreaseCreatesAdjustmentAndTriggersShoppingAutoAdd() {
        Product product = new Product();
        product.setAutoAddShopping(true);
        product.setReorderQuantity(BigDecimal.valueOf(2));
        product.setMinimumQuantityThreshold(BigDecimal.ONE);

        UnitOfMeasure unit = new UnitOfMeasure();
        StoragePlace storagePlace = new StoragePlace();
        StockEntry entry = new StockEntry();
        entry.setProduct(product);
        entry.setUnit(unit);
        entry.setStoragePlace(storagePlace);
        entry.setStatus(StockStatus.AVAILABLE);
        entry.setQuantity(BigDecimal.valueOf(3));

        when(stockEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(adjustmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = inventoryService.adjust(5L, new AdjustStockRequest(BigDecimal.valueOf(-3), "COUNT", "missing", "tester", null));

        assertEquals(BigDecimal.ZERO, response.quantity());
        assertEquals(StockStatus.CONSUMED, response.status());
        verify(adjustmentRepository).save(any(InventoryAdjustment.class));
        verify(shoppingListService).autoAddIfMissing(eq(product), eq(BigDecimal.valueOf(2)));
        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertEquals("STOCK_ADJUST", eventCaptor.getValue().eventType());
    }

    private StockEntry availableEntry(Product product, BigDecimal quantity, long id) {
        StockEntry entry = new StockEntry();
        entry.setProduct(product);
        entry.setUnit(product.getDefaultUnit());
        entry.setStoragePlace(new StoragePlace());
        entry.setQuantity(quantity);
        entry.setStatus(StockStatus.AVAILABLE);
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }

    private Product autoAddProduct(BigDecimal threshold) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 7L);
        product.setName("Milk");
        product.setAutoAddShopping(true);
        product.setMinimumQuantityThreshold(threshold);
        product.setReorderQuantity(BigDecimal.valueOf(2));
        UnitOfMeasure liter = new UnitOfMeasure();
        ReflectionTestUtils.setField(liter, "id", 3L);
        product.setDefaultUnit(liter);
        return product;
    }

    @Test
    void consumeMoreThanAvailableIsRejected() {
        StockEntry entry = availableEntry(autoAddProduct(null), BigDecimal.valueOf(2), 10L);
        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

        assertThrows(IllegalArgumentException.class,
            () -> inventoryService.consume(10L, new ConsumeStockRequest(BigDecimal.TEN)));
        assertEquals(BigDecimal.valueOf(2), entry.getQuantity());
        verify(stockEntryRepository, never()).save(any());
    }

    @Test
    void emptyingOneBatchDoesNotReplenishWhileOtherBatchesCoverThreshold() {
        Product product = autoAddProduct(BigDecimal.ONE);
        StockEntry entry = availableEntry(product, BigDecimal.ONE, 10L);
        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockEntryRepository.sumQuantityByProductAndStatusExcluding(7L, StockStatus.AVAILABLE, 10L))
            .thenReturn(BigDecimal.valueOf(5));
        when(stockEntryRepository.sumQuantityByProductStatusAndUnitExcluding(7L, StockStatus.AVAILABLE, 3L, 10L))
            .thenReturn(BigDecimal.valueOf(5));

        inventoryService.consume(10L, new ConsumeStockRequest(BigDecimal.ONE));

        verify(shoppingListService, never()).autoAddIfMissing(any(), any());
        verify(notificationService, never()).notifyOutOfStock(any(), any());
    }

    @Test
    void replenishesWhenTotalStockDropsToThreshold() {
        Product product = autoAddProduct(BigDecimal.valueOf(2));
        StockEntry entry = availableEntry(product, BigDecimal.valueOf(3), 10L);
        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockEntryRepository.sumQuantityByProductAndStatusExcluding(7L, StockStatus.AVAILABLE, 10L))
            .thenReturn(BigDecimal.ONE);
        when(stockEntryRepository.sumQuantityByProductStatusAndUnitExcluding(7L, StockStatus.AVAILABLE, 3L, 10L))
            .thenReturn(BigDecimal.ONE);

        inventoryService.consume(10L, new ConsumeStockRequest(BigDecimal.valueOf(2)));

        verify(shoppingListService).autoAddIfMissing(product, BigDecimal.valueOf(2));
        verify(notificationService, never()).notifyOutOfStock(any(), any());
    }

    @Test
    void discardOfLastBatchNotifiesOutOfStockAndReplenishes() {
        Product product = autoAddProduct(null);
        StockEntry entry = availableEntry(product, BigDecimal.ONE, 10L);
        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockEntryRepository.sumQuantityByProductAndStatusExcluding(7L, StockStatus.AVAILABLE, 10L))
            .thenReturn(BigDecimal.ZERO);

        var response = inventoryService.discard(10L);

        assertEquals(StockStatus.DISCARDED, response.status());
        verify(notificationService).notifyOutOfStock("Milk", 10L);
        verify(shoppingListService).autoAddIfMissing(product, BigDecimal.valueOf(2));
    }

    @Test
    void discardOfAlreadyConsumedBatchIsRejected() {
        StockEntry entry = availableEntry(autoAddProduct(null), BigDecimal.ZERO, 10L);
        entry.setStatus(StockStatus.CONSUMED);
        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

        assertThrows(IllegalArgumentException.class, () -> inventoryService.discard(10L));
        verify(stockEntryRepository, never()).save(any());
    }

    @Test
    void batchesInOtherUnitsAreNotComparedWithThreshold() {
        Product product = autoAddProduct(BigDecimal.valueOf(500));
        StockEntry entryInPacks = availableEntry(product, BigDecimal.valueOf(3), 10L);
        UnitOfMeasure pack = new UnitOfMeasure();
        ReflectionTestUtils.setField(pack, "id", 9L);
        entryInPacks.setUnit(pack); // threshold is "500" in the default unit, the batch is counted in packs
        when(stockEntryRepository.findById(10L)).thenReturn(Optional.of(entryInPacks));
        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockEntryRepository.sumQuantityByProductAndStatusExcluding(7L, StockStatus.AVAILABLE, 10L))
            .thenReturn(BigDecimal.ZERO);

        inventoryService.consume(10L, new ConsumeStockRequest(BigDecimal.ONE));

        verify(shoppingListService, never()).autoAddIfMissing(any(), any());
    }
}
