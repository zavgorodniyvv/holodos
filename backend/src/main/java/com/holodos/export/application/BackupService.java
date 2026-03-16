package com.holodos.export.application;

import com.holodos.catalog.domain.*;
import com.holodos.catalog.infrastructure.*;
import com.holodos.export.domain.BackupSnapshot;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BackupService {
    private final StoragePlaceRepository storagePlaceRepository;
    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;

    public BackupService(StoragePlaceRepository storagePlaceRepository, UnitRepository unitRepository,
                         CategoryRepository categoryRepository, StoreRepository storeRepository,
                         ProductRepository productRepository, ShoppingListItemRepository shoppingListItemRepository) {
        this.storagePlaceRepository = storagePlaceRepository;
        this.unitRepository = unitRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
    }

    @Transactional(readOnly = true)
    public BackupSnapshot exportSnapshot() {
        return new BackupSnapshot(
            OffsetDateTime.now(),
            storagePlaceRepository.findAll().stream().sorted(Comparator.comparing(StoragePlace::getName)).map(s ->
                new BackupSnapshot.StoragePlaceItem(s.getName(), s.getDescription(), s.getIcon(), s.getColor(), s.getSortOrder(), s.isActive())).toList(),
            unitRepository.findAll().stream().sorted(Comparator.comparing(UnitOfMeasure::getCode)).map(u ->
                new BackupSnapshot.UnitItem(u.getCode(), u.getName(), u.getShortName(), u.getUnitType(), u.isActive())).toList(),
            categoryRepository.findAll().stream().sorted(Comparator.comparing(Category::getName)).map(c ->
                new BackupSnapshot.CategoryItem(c.getName(), c.getDescription(), c.getIcon(), c.getColor(), c.getSortOrder(), c.isActive())).toList(),
            storeRepository.findAll().stream().sorted(Comparator.comparing(Store::getName)).map(s ->
                new BackupSnapshot.StoreItem(s.getName(), s.getDescription(), s.getIcon(), s.getColor(), s.getSortOrder(), s.isActive())).toList(),
            productRepository.findAll().stream().sorted(Comparator.comparing(Product::getName)).map(p ->
                new BackupSnapshot.ProductItem(
                    p.getName(), p.getCategory().getName(), p.getDefaultUnit().getCode(), p.getDefaultStoragePlace().getName(),
                    p.getDefaultStore() == null ? null : p.getDefaultStore().getName(), p.getPhotoKey(), p.getDescription(), p.getShelfLifeDays(),
                    p.getMinimumQuantityThreshold(), p.getReorderQuantity(), p.isAutoAddShopping(), p.getBarcode(), p.getNote(), p.isActive()
                )).toList(),
            shoppingListItemRepository.findAll().stream().sorted(Comparator.comparing(ShoppingListItem::getTitle)).map(i ->
                new BackupSnapshot.ShoppingItem(
                    i.getTitle(), i.getQuantity(), i.getUnit() == null ? null : i.getUnit().getCode(), i.getStore() == null ? null : i.getStore().getName(),
                    i.getStatus().name(), i.getSource().name(), i.getComment(), i.getSortOrder()
                )).toList()
        );
    }

    public RestoreResult restoreSnapshot(BackupSnapshot snapshot, boolean clearExisting) {
        if (clearExisting) {
            shoppingListItemRepository.deleteAllInBatch();
            productRepository.deleteAllInBatch();
            storeRepository.deleteAllInBatch();
            categoryRepository.deleteAllInBatch();
            unitRepository.deleteAllInBatch();
            storagePlaceRepository.deleteAllInBatch();
        }

        Map<String, StoragePlace> storageByName = upsertStorage(snapshot.storagePlaces()).stream()
            .collect(java.util.stream.Collectors.toMap(StoragePlace::getName, Function.identity()));
        Map<String, UnitOfMeasure> unitsByCode = upsertUnits(snapshot.units()).stream()
            .collect(java.util.stream.Collectors.toMap(UnitOfMeasure::getCode, Function.identity()));
        Map<String, Category> categoriesByName = upsertCategories(snapshot.categories()).stream()
            .collect(java.util.stream.Collectors.toMap(Category::getName, Function.identity()));
        Map<String, Store> storesByName = upsertStores(snapshot.stores()).stream()
            .collect(java.util.stream.Collectors.toMap(Store::getName, Function.identity()));

        for (BackupSnapshot.ProductItem p : snapshot.products()) {
            Product product = new Product();
            product.setName(p.name());
            product.setCategory(required(categoriesByName, p.categoryName(), "Category"));
            product.setDefaultUnit(required(unitsByCode, p.defaultUnitCode(), "Unit"));
            product.setDefaultStoragePlace(required(storageByName, p.defaultStoragePlaceName(), "StoragePlace"));
            product.setDefaultStore(p.defaultStoreName() == null ? null : required(storesByName, p.defaultStoreName(), "Store"));
            product.setPhotoKey(p.photoKey());
            product.setDescription(p.description());
            product.setShelfLifeDays(p.shelfLifeDays());
            product.setMinimumQuantityThreshold(p.minimumQuantityThreshold());
            product.setReorderQuantity(p.reorderQuantity());
            product.setAutoAddShopping(Boolean.TRUE.equals(p.autoAddShopping()));
            product.setBarcode(p.barcode());
            product.setNote(p.note());
            product.setActive(Boolean.TRUE.equals(p.active()));
            productRepository.save(product);
        }

        for (BackupSnapshot.ShoppingItem item : snapshot.shoppingItems()) {
            ShoppingListItem s = new ShoppingListItem();
            s.setTitle(item.title());
            s.setQuantity(item.quantity());
            s.setUnit(item.unitCode() == null ? null : required(unitsByCode, item.unitCode(), "Unit"));
            s.setStore(item.storeName() == null ? null : required(storesByName, item.storeName(), "Store"));
            s.setStatus(parseStatus(item.status()));
            s.setSource(parseSource(item.source()));
            s.setComment(item.comment());
            s.setSortOrder(item.sortOrder() == null ? 0 : item.sortOrder());
            shoppingListItemRepository.save(s);
        }

        return new RestoreResult(
            snapshot.storagePlaces().size(),
            snapshot.units().size(),
            snapshot.categories().size(),
            snapshot.stores().size(),
            snapshot.products().size(),
            snapshot.shoppingItems().size()
        );
    }

    private List<StoragePlace> upsertStorage(List<BackupSnapshot.StoragePlaceItem> list) {
        return list.stream().map(i -> {
            StoragePlace s = new StoragePlace();
            s.setName(i.name()); s.setDescription(i.description()); s.setIcon(i.icon()); s.setColor(i.color()); s.setSortOrder(i.sortOrder() == null ? 0 : i.sortOrder()); s.setActive(Boolean.TRUE.equals(i.active()));
            return storagePlaceRepository.save(s);
        }).toList();
    }

    private List<UnitOfMeasure> upsertUnits(List<BackupSnapshot.UnitItem> list) {
        return list.stream().map(i -> {
            UnitOfMeasure u = new UnitOfMeasure();
            u.setCode(i.code()); u.setName(i.name()); u.setShortName(i.shortName()); u.setUnitType(i.unitType()); u.setActive(Boolean.TRUE.equals(i.active()));
            return unitRepository.save(u);
        }).toList();
    }

    private List<Category> upsertCategories(List<BackupSnapshot.CategoryItem> list) {
        return list.stream().map(i -> {
            Category c = new Category();
            c.setName(i.name()); c.setDescription(i.description()); c.setIcon(i.icon()); c.setColor(i.color()); c.setSortOrder(i.sortOrder() == null ? 0 : i.sortOrder()); c.setActive(Boolean.TRUE.equals(i.active()));
            return categoryRepository.save(c);
        }).toList();
    }

    private List<Store> upsertStores(List<BackupSnapshot.StoreItem> list) {
        return list.stream().map(i -> {
            Store s = new Store();
            s.setName(i.name()); s.setDescription(i.description()); s.setIcon(i.icon()); s.setColor(i.color()); s.setSortOrder(i.sortOrder() == null ? 0 : i.sortOrder()); s.setActive(Boolean.TRUE.equals(i.active()));
            return storeRepository.save(s);
        }).toList();
    }

    private <T> T required(Map<String, T> map, String key, String type) {
        T value = map.get(key);
        if (value == null) throw new IllegalArgumentException(type + " not found in snapshot: " + key);
        return value;
    }

    private ShoppingItemStatus parseStatus(String s) {
        try { return ShoppingItemStatus.valueOf(s); } catch (Exception ignored) { return ShoppingItemStatus.ACTIVE; }
    }

    private ShoppingItemSource parseSource(String s) {
        try { return ShoppingItemSource.valueOf(s); } catch (Exception ignored) { return ShoppingItemSource.IMPORT; }
    }

    public record RestoreResult(int storagePlaces, int units, int categories, int stores, int products, int shoppingItems) {}
}
