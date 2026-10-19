package com.holodos.export.application;

import com.holodos.catalog.domain.*;
import com.holodos.catalog.infrastructure.*;
import com.holodos.export.domain.BackupSnapshot;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.InventoryAdjustmentRepository;
import com.holodos.inventory.infrastructure.InventoryMovementRepository;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.media.infrastructure.MediaObjectRepository;
import com.holodos.purchases.infrastructure.PurchaseEventRepository;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final StockEntryRepository stockEntryRepository;
    private final PurchaseEventRepository purchaseEventRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final MediaObjectRepository mediaObjectRepository;

    public BackupService(StoragePlaceRepository storagePlaceRepository, UnitRepository unitRepository,
                         CategoryRepository categoryRepository, StoreRepository storeRepository,
                         ProductRepository productRepository, ShoppingListItemRepository shoppingListItemRepository,
                         StockEntryRepository stockEntryRepository, PurchaseEventRepository purchaseEventRepository,
                         InventoryMovementRepository movementRepository, InventoryAdjustmentRepository adjustmentRepository,
                         MediaObjectRepository mediaObjectRepository) {
        this.storagePlaceRepository = storagePlaceRepository;
        this.unitRepository = unitRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.purchaseEventRepository = purchaseEventRepository;
        this.movementRepository = movementRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.mediaObjectRepository = mediaObjectRepository;
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
                    i.getStatus().name(), i.getSource().name(), i.getComment(), i.getSortOrder(),
                    i.getProduct() == null ? null : i.getProduct().getName()
                )).toList(),
            stockEntryRepository.findByStatus(StockStatus.AVAILABLE).stream().map(s ->
                new BackupSnapshot.StockItem(
                    s.getProduct().getName(), s.getQuantity(), s.getUnit().getCode(), s.getStoragePlace().getName(),
                    s.getAddedAt(), s.getPurchasedAt(), s.getExpiresAt(), s.getOpenedAt(), s.getComment()
                )).toList()
        );
    }

    /**
     * Restores a snapshot. Dictionaries and products are matched by name (units by code) and updated in place,
     * so restoring twice does not create duplicates. With {@code clearExisting} the current catalog, shopping list,
     * stock and their history are deleted first (children before parents, so foreign keys are satisfied).
     * Stock from the snapshot is restored only into an empty inventory to avoid doubling it.
     */
    public RestoreResult restoreSnapshot(BackupSnapshot snapshot, boolean clearExisting) {
        if (clearExisting) {
            adjustmentRepository.deleteAllInBatch();
            movementRepository.deleteAllInBatch();
            purchaseEventRepository.deleteAllInBatch();
            stockEntryRepository.deleteAllInBatch();
            shoppingListItemRepository.deleteAllInBatch();
            productRepository.deleteAllInBatch();
            storeRepository.deleteAllInBatch();
            categoryRepository.deleteAllInBatch();
            unitRepository.deleteAllInBatch();
            storagePlaceRepository.deleteAllInBatch();
        }

        Map<String, StoragePlace> storageByName = new HashMap<>();
        for (BackupSnapshot.StoragePlaceItem i : nullToEmpty(snapshot.storagePlaces())) {
            StoragePlace s = storagePlaceRepository.findByName(i.name()).orElseGet(StoragePlace::new);
            s.setName(i.name()); s.setDescription(i.description()); s.setIcon(i.icon()); s.setColor(i.color());
            s.setSortOrder(i.sortOrder() == null ? 0 : i.sortOrder()); s.setActive(Boolean.TRUE.equals(i.active()));
            storageByName.put(i.name(), storagePlaceRepository.save(s));
        }
        Map<String, UnitOfMeasure> unitsByCode = new HashMap<>();
        for (BackupSnapshot.UnitItem i : nullToEmpty(snapshot.units())) {
            UnitOfMeasure u = unitRepository.findByCode(i.code()).orElseGet(UnitOfMeasure::new);
            u.setCode(i.code()); u.setName(i.name()); u.setShortName(i.shortName()); u.setUnitType(i.unitType());
            u.setActive(Boolean.TRUE.equals(i.active()));
            unitsByCode.put(i.code(), unitRepository.save(u));
        }
        Map<String, Category> categoriesByName = new HashMap<>();
        for (BackupSnapshot.CategoryItem i : nullToEmpty(snapshot.categories())) {
            Category c = categoryRepository.findByName(i.name()).orElseGet(Category::new);
            c.setName(i.name()); c.setDescription(i.description()); c.setIcon(i.icon()); c.setColor(i.color());
            c.setSortOrder(i.sortOrder() == null ? 0 : i.sortOrder()); c.setActive(Boolean.TRUE.equals(i.active()));
            categoriesByName.put(i.name(), categoryRepository.save(c));
        }
        Map<String, Store> storesByName = new HashMap<>();
        for (BackupSnapshot.StoreItem i : nullToEmpty(snapshot.stores())) {
            Store s = storeRepository.findByName(i.name()).orElseGet(Store::new);
            s.setName(i.name()); s.setDescription(i.description()); s.setIcon(i.icon()); s.setColor(i.color());
            s.setSortOrder(i.sortOrder() == null ? 0 : i.sortOrder()); s.setActive(Boolean.TRUE.equals(i.active()));
            storesByName.put(i.name(), storeRepository.save(s));
        }

        Map<String, Product> productsByName = new HashMap<>();
        for (BackupSnapshot.ProductItem p : nullToEmpty(snapshot.products())) {
            // Product names are not unique: after a clear every snapshot product is recreated as is;
            // when merging into existing data, a product is matched by name
            Product product = clearExisting ? new Product()
                : productRepository.findFirstByNameIgnoreCase(p.name()).orElseGet(Product::new);
            product.setName(p.name());
            product.setCategory(required(categoriesByName, p.categoryName(), "Category"));
            product.setDefaultUnit(required(unitsByCode, p.defaultUnitCode(), "Unit"));
            product.setDefaultStoragePlace(required(storageByName, p.defaultStoragePlaceName(), "StoragePlace"));
            product.setDefaultStore(p.defaultStoreName() == null ? null : required(storesByName, p.defaultStoreName(), "Store"));
            // Photo files are not part of the backup: keep a key only if it points at a media object we actually have
            product.setPhotoKey(p.photoKey() != null && mediaObjectRepository.existsByObjectKey(p.photoKey()) ? p.photoKey() : null);
            product.setDescription(p.description());
            product.setShelfLifeDays(p.shelfLifeDays());
            product.setMinimumQuantityThreshold(p.minimumQuantityThreshold());
            product.setReorderQuantity(p.reorderQuantity());
            product.setAutoAddShopping(Boolean.TRUE.equals(p.autoAddShopping()));
            product.setBarcode(p.barcode());
            product.setNote(p.note());
            product.setActive(Boolean.TRUE.equals(p.active()));
            productsByName.putIfAbsent(p.name(), productRepository.save(product));
        }

        Set<String> activeTitlesBeforeRestore = clearExisting ? Set.of()
            : shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE).stream()
                .map(ShoppingListItem::getTitle).collect(Collectors.toSet());
        int shoppingRestored = 0;
        for (BackupSnapshot.ShoppingItem item : nullToEmpty(snapshot.shoppingItems())) {
            ShoppingItemStatus status = parseStatus(item.status());
            // Skip only items already on the list before this restore; duplicates inside the snapshot are kept
            if (!clearExisting && status == ShoppingItemStatus.ACTIVE
                && activeTitlesBeforeRestore.contains(item.title())) {
                continue;
            }
            ShoppingListItem s = new ShoppingListItem();
            s.setTitle(item.title());
            s.setQuantity(item.quantity());
            s.setProduct(item.productName() == null ? null : productsByName.get(item.productName()));
            s.setUnit(item.unitCode() == null ? null : required(unitsByCode, item.unitCode(), "Unit"));
            s.setStore(item.storeName() == null ? null : required(storesByName, item.storeName(), "Store"));
            s.setStatus(status);
            s.setSource(parseSource(item.source()));
            s.setComment(item.comment());
            s.setSortOrder(item.sortOrder() == null ? 0 : item.sortOrder());
            shoppingListItemRepository.save(s);
            shoppingRestored++;
        }

        int stockRestored = 0;
        if (clearExisting || stockEntryRepository.count() == 0) {
            for (BackupSnapshot.StockItem item : nullToEmpty(snapshot.stockEntries())) {
                StockEntry entry = new StockEntry();
                entry.setProduct(required(productsByName, item.productName(), "Product"));
                entry.setQuantity(item.quantity());
                entry.setUnit(required(unitsByCode, item.unitCode(), "Unit"));
                entry.setStoragePlace(required(storageByName, item.storagePlaceName(), "StoragePlace"));
                entry.setAddedAt(item.addedAt() == null ? OffsetDateTime.now() : item.addedAt());
                entry.setPurchasedAt(item.purchasedAt());
                entry.setExpiresAt(item.expiresAt());
                entry.setOpenedAt(item.openedAt());
                entry.setComment(item.comment());
                entry.setStatus(StockStatus.AVAILABLE);
                stockEntryRepository.save(entry);
                stockRestored++;
            }
        }

        return new RestoreResult(
            storageByName.size(),
            unitsByCode.size(),
            categoriesByName.size(),
            storesByName.size(),
            productsByName.size(),
            shoppingRestored,
            stockRestored
        );
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
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

    public record RestoreResult(int storagePlaces, int units, int categories, int stores, int products, int shoppingItems, int stockEntries) {}
}
