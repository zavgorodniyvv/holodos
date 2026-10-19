package com.holodos.export.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.catalog.domain.*;
import com.holodos.catalog.infrastructure.*;
import com.holodos.export.domain.BackupSnapshot;
import com.holodos.inventory.infrastructure.InventoryAdjustmentRepository;
import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.media.infrastructure.MediaObjectRepository;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
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
    @Mock StockEntryRepository stockEntryRepository;
    @Mock PurchaseEventRepository purchaseEventRepository;
    @Mock InventoryMovementRepository movementRepository;
    @Mock InventoryAdjustmentRepository adjustmentRepository;
    @Mock MediaObjectRepository mediaObjectRepository;

    BackupService service;

    @BeforeEach
    void setUp() {
        service = new BackupService(storagePlaceRepository, unitRepository, categoryRepository, storeRepository, productRepository,
            shoppingListItemRepository, stockEntryRepository, purchaseEventRepository, movementRepository, adjustmentRepository,
            mediaObjectRepository);
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
            List.of(new BackupSnapshot.ShoppingItem("Bread", BigDecimal.ONE, "piece", "Supermarket", "ACTIVE", "MANUAL", null, 0, "Milk")),
            List.of(new BackupSnapshot.StockItem("Milk", BigDecimal.ONE, "piece", "Fridge", null, null, null, null, null))
        );

        when(stockEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.restoreSnapshot(snapshot, false);

        assertEquals(1, result.products());
        assertEquals(1, result.shoppingItems());
        assertEquals(1, result.stockEntries());
    }

    @Test
    void restoreUpdatesExistingDictionariesInsteadOfDuplicating() {
        StoragePlace existing = new StoragePlace();
        existing.setName("Fridge");
        when(storagePlaceRepository.findByName("Fridge")).thenReturn(Optional.of(existing));
        when(storagePlaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockEntryRepository.count()).thenReturn(5L);

        BackupSnapshot snapshot = new BackupSnapshot(OffsetDateTime.now(),
            List.of(new BackupSnapshot.StoragePlaceItem("Fridge", "updated", null, null, 1, true)),
            List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(new BackupSnapshot.StockItem("Milk", BigDecimal.ONE, "piece", "Fridge", null, null, null, null, null)));

        var result = service.restoreSnapshot(snapshot, false);

        verify(storagePlaceRepository).save(existing);
        assertEquals("updated", existing.getDescription());
        // Inventory already has stock: snapshot stock is not added on top of it
        assertEquals(0, result.stockEntries());
        verify(stockEntryRepository, never()).save(any());
    }

    @Test
    void clearExistingDeletesChildrenBeforeParents() {
        BackupSnapshot empty = new BackupSnapshot(OffsetDateTime.now(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), null);

        service.restoreSnapshot(empty, true);

        var order = inOrder(adjustmentRepository, movementRepository, purchaseEventRepository, stockEntryRepository,
            shoppingListItemRepository, productRepository, storeRepository, categoryRepository, unitRepository, storagePlaceRepository);
        order.verify(adjustmentRepository).deleteAllInBatch();
        order.verify(movementRepository).deleteAllInBatch();
        order.verify(purchaseEventRepository).deleteAllInBatch();
        order.verify(stockEntryRepository).deleteAllInBatch();
        order.verify(shoppingListItemRepository).deleteAllInBatch();
        order.verify(productRepository).deleteAllInBatch();
        order.verify(storeRepository).deleteAllInBatch();
        order.verify(categoryRepository).deleteAllInBatch();
        order.verify(unitRepository).deleteAllInBatch();
        order.verify(storagePlaceRepository).deleteAllInBatch();
    }

    @Test
    void clearExistingRestoresProductsAndItemsWithDuplicateNames() {
        when(storagePlaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(unitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(shoppingListItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        BackupSnapshot.ProductItem milk = new BackupSnapshot.ProductItem("Milk", "Food", "piece", "Fridge", null, null,
            null, null, null, null, true, null, null, true);
        BackupSnapshot.ShoppingItem bread = new BackupSnapshot.ShoppingItem("Bread", BigDecimal.ONE, null, null, "ACTIVE", "MANUAL", null, 0, null);

        BackupSnapshot snapshot = new BackupSnapshot(OffsetDateTime.now(),
            List.of(new BackupSnapshot.StoragePlaceItem("Fridge", null, null, null, 0, true)),
            List.of(new BackupSnapshot.UnitItem("piece", "Piece", "pcs", UnitType.COUNT, true)),
            List.of(new BackupSnapshot.CategoryItem("Food", null, null, null, 0, true)),
            List.of(), List.of(milk, milk), List.of(bread, bread), null);

        var result = service.restoreSnapshot(snapshot, true);

        verify(productRepository, times(2)).save(any());
        assertEquals(2, result.shoppingItems());
        verify(productRepository, never()).findFirstByNameIgnoreCase(any());
    }
}
