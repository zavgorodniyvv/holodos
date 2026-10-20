package com.holodos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.holodos.catalog.api.CatalogDtos.ProductUpsertRequest;
import com.holodos.catalog.application.ProductService;
import com.holodos.catalog.infrastructure.CategoryRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.domain.ConflictException;
import com.holodos.export.application.BackupService;
import com.holodos.inventory.api.InventoryDtos.AddStockRequest;
import com.holodos.inventory.application.InventoryService;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.notifications.application.InventoryNotificationScheduler;
import com.holodos.purchases.api.PurchaseDtos.ProcessPurchaseRequest;
import com.holodos.purchases.application.PurchaseService;
import com.holodos.reports.api.ReportDtos.ReportFilter;
import com.holodos.reports.application.ReportsService;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemUpsertRequest;
import com.holodos.shopping.application.ShoppingListService;
import com.holodos.shopping.domain.ShoppingItemSource;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the whole application against a real PostgreSQL: Flyway migrations + ddl validation, JSONB audit
 * writes after commit, the JPQL/derived queries and the restore delete order are exercised for real.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApplicationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.security.oauth2.client.registration.google.client-id", () -> "test");
        registry.add("spring.security.oauth2.client.registration.google.client-secret", () -> "test");
        String mediaRoot = Files.createTempDirectory("holodos-media").toString();
        registry.add("holodos.media.root-path", () -> mediaRoot);
        // Real OpenAI classifier pointed at a closed port: proves the bean wires and that AI failures fall back to defaults
        registry.add("holodos.integrations.openai.enabled", () -> "true");
        registry.add("holodos.integrations.openai.api-key", () -> "test");
        registry.add("holodos.integrations.openai.base-url", () -> "http://127.0.0.1:9");
    }

    @Autowired ProductService productService;
    @Autowired InventoryService inventoryService;
    @Autowired ShoppingListService shoppingListService;
    @Autowired PurchaseService purchaseService;
    @Autowired BackupService backupService;
    @Autowired ReportsService reportsService;
    @Autowired InventoryNotificationScheduler notificationScheduler;
    @Autowired CategoryRepository categoryRepository;
    @Autowired UnitRepository unitRepository;
    @Autowired StoragePlaceRepository storagePlaceRepository;
    @Autowired StockEntryRepository stockEntryRepository;
    @Autowired JdbcTemplate jdbc;

    @Test
    void endToEndFlowsWorkAgainstPostgres() throws Exception {
        long categoryId = categoryRepository.findByName("Food").orElseThrow().getId();
        long unitId = unitRepository.findByCode("l").orElseThrow().getId();
        long fridgeId = storagePlaceRepository.findByName("Fridge").orElseThrow().getId();

        var milk = productService.create(new ProductUpsertRequest("Milk", categoryId, unitId, fridgeId, null, null, null,
            7, BigDecimal.ONE, BigDecimal.valueOf(2), true, null, null, true));
        var stock = inventoryService.addStock(new AddStockRequest(milk.id(), BigDecimal.valueOf(3), unitId, fridgeId,
            null, null, OffsetDateTime.now().minusDays(1), null, null, null, null));

        // Audit log: written after commit in its own transaction, payload stored as real JSONB
        Integer stockAddRows = waitForOperationLog("STOCK_ADD");
        assertEquals(1, stockAddRows);
        String productIdInPayload = jdbc.queryForObject(
            "select payload->>'productId' from operation_log where event_type = 'STOCK_ADD'", String.class);
        assertEquals(String.valueOf(milk.id()), productIdInPayload);

        // Consuming below the threshold puts milk on the shopping list exactly once
        inventoryService.consume(stock.id(), new com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest(BigDecimal.valueOf(2)));
        inventoryService.consume(stock.id(), new com.holodos.inventory.api.InventoryDtos.ConsumeStockRequest(BigDecimal.valueOf(0.5)));
        Integer activeMilkItems = jdbc.queryForObject(
            "select count(*) from shopping_list_items where product_id = ? and status = 'ACTIVE'", Integer.class, milk.id());
        assertEquals(1, activeMilkItems);
        BigDecimal listedQuantity = jdbc.queryForObject(
            "select quantity from shopping_list_items where product_id = ? and status = 'ACTIVE'", BigDecimal.class, milk.id());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(listedQuantity));

        // Purchasing the same item twice is rejected
        var bread = shoppingListService.create(new ShoppingItemUpsertRequest(milk.id(), "Milk again", BigDecimal.ONE, unitId, null,
            ShoppingItemSource.MANUAL, null, 0));
        ProcessPurchaseRequest purchase = new ProcessPurchaseRequest(bread.id(), BigDecimal.ONE, fridgeId, null, null, null);
        purchaseService.processPurchase(purchase);
        assertThrows(ConflictException.class, () -> purchaseService.processPurchase(purchase));

        // Reports and the notification scan run their queries without errors
        var report = reportsService.build(new ReportFilter(null, null, null, null, null));
        assertEquals(1L, report.purchasesCount());
        assertTrue(report.expiredCount() >= 1);
        notificationScheduler.scanExpiryAndOldItems();
        Integer expiredNotifications = jdbc.queryForObject(
            "select count(*) from notifications where type = 'EXPIRED'", Integer.class);
        assertTrue(expiredNotifications >= 1);

        // Full restore with clearExisting works while stock and purchase history exist (used to fail on FKs)
        var snapshot = backupService.exportSnapshot();
        long availableBefore = stockEntryRepository.findByStatus(StockStatus.AVAILABLE).size();
        var result = backupService.restoreSnapshot(snapshot, true);
        assertEquals(availableBefore, result.stockEntries());
        assertEquals(availableBefore, stockEntryRepository.findByStatus(StockStatus.AVAILABLE).size());

        // Restoring again without clearing does not duplicate the catalog
        int categoriesBefore = categoryRepository.findAll().size();
        backupService.restoreSnapshot(snapshot, false);
        assertEquals(categoriesBefore, categoryRepository.findAll().size());

        // Adding by name creates the product once (Cyrillic, case-insensitive); a free-text item is linked to it on purchase
        // (the restore above re-created the dictionaries, so their ids changed)
        long literId = unitRepository.findByCode("l").orElseThrow().getId();
        long restoredFridgeId = storagePlaceRepository.findByName("Fridge").orElseThrow().getId();
        var kefirStock = inventoryService.addStock(new AddStockRequest(null, BigDecimal.ONE, literId, restoredFridgeId,
            null, null, null, null, "кефир", null, 5));
        var kefirItem = shoppingListService.create(new ShoppingItemUpsertRequest(null, "КЕФИР", BigDecimal.ONE, null, null,
            ShoppingItemSource.KEEP_SYNC, null, 0));
        purchaseService.processPurchase(new ProcessPurchaseRequest(kefirItem.id(), BigDecimal.ONE, restoredFridgeId, null, null, null));
        assertEquals(1, jdbc.queryForObject("select count(*) from products where lower(name) = 'кефир'", Integer.class));
        assertEquals(kefirStock.productId(),
            jdbc.queryForObject("select product_id from shopping_list_items where id = ?", Long.class, kefirItem.id()));
        assertTrue(kefirStock.expiresAt() != null, "expiry derived from the shelf life of the new product");
    }

    private Integer waitForOperationLog(String eventType) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            Integer rows = jdbc.queryForObject("select count(*) from operation_log where event_type = ?", Integer.class, eventType);
            if (rows != null && rows > 0) {
                return rows;
            }
            Thread.sleep(100);
        }
        return 0;
    }
}
