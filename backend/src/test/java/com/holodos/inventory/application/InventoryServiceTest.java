package com.holodos.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.OperationLogService;
import com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.shopping.application.ShoppingListService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock StockEntryRepository stockEntryRepository;
    @Mock InventoryMovementRepository movementRepository;
    @Mock ProductRepository productRepository;
    @Mock UnitRepository unitRepository;
    @Mock StoragePlaceRepository storagePlaceRepository;
    @Mock ShoppingListService shoppingListService;
    @Mock OperationLogService operationLogService;

    InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(stockEntryRepository, movementRepository, productRepository, unitRepository, storagePlaceRepository, shoppingListService, operationLogService);
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
        verify(operationLogService).log(eq("STOCK_CONSUME"), eq("StockEntry"), anyString(), anyMap());
    }
}
