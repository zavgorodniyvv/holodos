package com.holodos.reports.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.common.domain.OperationLog;
import com.holodos.common.infrastructure.OperationLogRepository;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.reports.api.ReportDtos.ReportFilter;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {
    @Mock StockEntryRepository stockEntryRepository;
    @Mock PurchaseEventRepository purchaseEventRepository;
    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock OperationLogRepository operationLogRepository;

    ReportsService service;

    @BeforeEach
    void setUp() {
        service = new ReportsService(stockEntryRepository, purchaseEventRepository, shoppingListItemRepository, operationLogRepository);
    }

    @Test
    void buildCsvContainsMetrics() {
        StoragePlace sp = new StoragePlace();
        sp.setName("Fridge");
        Category category = new Category();
        category.setName("Food");
        Product product = new Product();
        product.setCategory(category);

        StockEntry stock = new StockEntry();
        stock.setProduct(product);
        stock.setStoragePlace(sp);
        stock.setStatus(StockStatus.AVAILABLE);
        stock.setAddedAt(OffsetDateTime.now());

        OperationLog log = new OperationLog();
        log.setEventType("STOCK_ADD");

        when(stockEntryRepository.findAll()).thenReturn(List.of(stock));
        when(purchaseEventRepository.findAll()).thenReturn(List.of());
        when(shoppingListItemRepository.findAll()).thenReturn(List.of());
        when(operationLogRepository.findAll()).thenReturn(List.of(log));

        String csv = service.buildCsv(new ReportFilter(null, null, null, null, null));

        assertTrue(csv.contains("metric,value"));
        assertTrue(csv.contains("inventoryByStoragePlace.Fridge"));
    }
}
