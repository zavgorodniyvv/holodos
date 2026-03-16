package com.holodos.export.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.catalog.domain.*;
import com.holodos.catalog.infrastructure.*;
import com.holodos.export.domain.BackupSnapshot;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {
    @Mock StoragePlaceRepository storagePlaceRepository;
    @Mock UnitRepository unitRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock StoreRepository storeRepository;
    @Mock ProductRepository productRepository;
    @Mock ShoppingListItemRepository shoppingListItemRepository;

    BackupService service;

    @BeforeEach
    void setUp() {
        service = new BackupService(storagePlaceRepository, unitRepository, categoryRepository, storeRepository, productRepository, shoppingListItemRepository);
    }

    @Test
    void exportSnapshotReturnsData() {
        StoragePlace sp = new StoragePlace(); sp.setName("Fridge");
        UnitOfMeasure u = new UnitOfMeasure(); u.setCode("piece"); u.setName("Piece"); u.setShortName("pcs"); u.setUnitType(UnitType.COUNT);
        Category c = new Category(); c.setName("Food");
        Store st = new Store(); st.setName("Supermarket");
        Product p = new Product(); p.setName("Milk"); p.setCategory(c); p.setDefaultUnit(u); p.setDefaultStoragePlace(sp);

        when(storagePlaceRepository.findAll()).thenReturn(List.of(sp));
        when(unitRepository.findAll()).thenReturn(List.of(u));
        when(categoryRepository.findAll()).thenReturn(List.of(c));
        when(storeRepository.findAll()).thenReturn(List.of(st));
        when(productRepository.findAll()).thenReturn(List.of(p));
        when(shoppingListItemRepository.findAll()).thenReturn(List.of());

        BackupSnapshot snapshot = service.exportSnapshot();

        assertEquals(1, snapshot.products().size());
        assertEquals("Milk", snapshot.products().get(0).name());
    }

    @Test
    void restoreSnapshotReturnsCounts() {
        when(storagePlaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(unitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(storeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(shoppingListItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BackupSnapshot snapshot = new BackupSnapshot(
            OffsetDateTime.now(),
            List.of(new BackupSnapshot.StoragePlaceItem("Fridge", null, null, null, 0, true)),
            List.of(new BackupSnapshot.UnitItem("piece", "Piece", "pcs", UnitType.COUNT, true)),
            List.of(new BackupSnapshot.CategoryItem("Food", null, null, null, 0, true)),
            List.of(new BackupSnapshot.StoreItem("Supermarket", null, null, null, 0, true)),
            List.of(new BackupSnapshot.ProductItem("Milk", "Food", "piece", "Fridge", "Supermarket", null, null, null, null, BigDecimal.ONE, true, null, null, true)),
            List.of(new BackupSnapshot.ShoppingItem("Bread", BigDecimal.ONE, "piece", "Supermarket", "ACTIVE", "MANUAL", null, 0))
        );

        var result = service.restoreSnapshot(snapshot, false);

        assertEquals(1, result.products());
        assertEquals(1, result.shoppingItems());
    }
}
