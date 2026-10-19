package com.holodos.reports.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.common.infrastructure.OperationLogRepository;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.reports.api.ReportDtos.ReportFilter;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import com.holodos.settings.application.UserSettingsService;
import com.holodos.settings.domain.UserSettings;
import com.holodos.shopping.domain.ShoppingItemStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
    @Mock UserSettingsService userSettingsService;

    ReportsService service;

    @BeforeEach
    void setUp() {
        service = new ReportsService(stockEntryRepository, purchaseEventRepository, shoppingListItemRepository, operationLogRepository,
            userSettingsService, "UTC");
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

        when(stockEntryRepository.findWithProductByStatus(StockStatus.AVAILABLE)).thenReturn(List.of(stock));
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE)).thenReturn(List.of());
        when(operationLogRepository.countByEventTypeBetween(any(), any()))
            .thenReturn(List.<Object[]>of(new Object[] {"STOCK_ADD", 3L}));
        when(userSettingsService.getOrDefault("default")).thenReturn(new UserSettings());
        String csv = service.buildCsv(new ReportFilter(null, null, null, null, null));

        assertTrue(csv.contains("metric,value"));
        assertTrue(csv.contains("inventoryByStoragePlace.Fridge"));
        assertTrue(csv.contains("operationLogByEvent.STOCK_ADD,3"));
    }

    @Test
    void expiredAndExpiringAreCountedByCalendarDate() {
        StoragePlace sp = new StoragePlace();
        sp.setName("Fridge");
        Category category = new Category();
        category.setName("Food");
        Product product = new Product();
        product.setCategory(category);
        OffsetDateTime todayUtc = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);

        StockEntry expired = stock(product, sp, todayUtc.minusDays(1));
        StockEntry expiresToday = stock(product, sp, todayUtc);
        StockEntry expiresLater = stock(product, sp, todayUtc.plusDays(30));

        when(stockEntryRepository.findWithProductByStatus(StockStatus.AVAILABLE))
            .thenReturn(List.of(expired, expiresToday, expiresLater));
        when(userSettingsService.getOrDefault("default")).thenReturn(new UserSettings());

        var report = service.build(new ReportFilter(null, null, null, null, null));

        assertEquals(1, report.expiredCount());
        assertEquals(1, report.expiringSoonCount());
        assertEquals(3L, report.inventoryByStoragePlace().get("Fridge"));
    }

    private static StockEntry stock(Product product, StoragePlace sp, OffsetDateTime expiresAt) {
        StockEntry s = new StockEntry();
        s.setProduct(product);
        s.setStoragePlace(sp);
        s.setStatus(StockStatus.AVAILABLE);
        s.setAddedAt(OffsetDateTime.now());
        s.setExpiresAt(expiresAt);
        return s;
    }
}
