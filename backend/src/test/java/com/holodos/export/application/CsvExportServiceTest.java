package com.holodos.export.application;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.domain.UnitType;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.common.domain.OperationLog;
import com.holodos.common.infrastructure.OperationLogRepository;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvExportServiceTest {
    @Mock ProductRepository productRepository;
    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock OperationLogRepository operationLogRepository;

    CsvExportService service;

    @BeforeEach
    void setUp() {
        service = new CsvExportService(productRepository, shoppingListItemRepository, operationLogRepository);
    }

    @Test
    void exportProductsContainsHeaderAndRow() {
        Product p = new Product();
        p.setName("Milk");
        p.setActive(true);
        Category c = new Category(); c.setName("Food");
        UnitOfMeasure u = new UnitOfMeasure(); u.setCode("piece"); u.setUnitType(UnitType.COUNT);
        StoragePlace s = new StoragePlace(); s.setName("Fridge");
        p.setCategory(c); p.setDefaultUnit(u); p.setDefaultStoragePlace(s);

        when(productRepository.findAll()).thenReturn(List.of(p));

        String csv = service.exportProducts();
        assertTrue(csv.contains("id,name,category"));
        assertTrue(csv.contains("Milk"));
    }

    @Test
    void exportShoppingAndOpLogContainRows() {
        ShoppingListItem i = new ShoppingListItem();
        i.setTitle("Bread");
        i.setQuantity(BigDecimal.ONE);
        i.setStatus(ShoppingItemStatus.ACTIVE);
        i.setSource(ShoppingItemSource.MANUAL);
        when(shoppingListItemRepository.findAll()).thenReturn(List.of(i));

        OperationLog log = new OperationLog();
        log.setEventType("SHOPPING_CREATE");
        log.setEntityType("ShoppingListItem");
        when(operationLogRepository.findAll()).thenReturn(List.of(log));

        assertTrue(service.exportShoppingList().contains("Bread"));
        assertTrue(service.exportOperationLog().contains("SHOPPING_CREATE"));
    }
}
