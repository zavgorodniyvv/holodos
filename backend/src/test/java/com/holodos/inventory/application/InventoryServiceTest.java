package com.holodos.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(stockEntryRepository, movementRepository, productRepository, unitRepository,
            storagePlaceRepository, shoppingListService, domainEventPublisher, adjustmentRepository);
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
}
