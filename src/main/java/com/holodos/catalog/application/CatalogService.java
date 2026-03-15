package com.holodos.catalog.application;

import com.holodos.catalog.api.CatalogDtos;
import com.holodos.catalog.domain.model.Category;
import com.holodos.catalog.domain.model.Product;
import com.holodos.catalog.domain.model.StoragePlace;
import com.holodos.catalog.domain.model.Store;
import com.holodos.catalog.domain.model.UnitOfMeasure;
import com.holodos.catalog.domain.repository.CategoryRepository;
import com.holodos.catalog.domain.repository.ProductRepository;
import com.holodos.catalog.domain.repository.StoragePlaceRepository;
import com.holodos.catalog.domain.repository.StoreRepository;
import com.holodos.catalog.domain.repository.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogService {

    private final StoragePlaceRepository storagePlaceRepository;
    private final UnitRepository unitRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    public CatalogService(StoragePlaceRepository storagePlaceRepository,
                          UnitRepository unitRepository,
                          CategoryRepository categoryRepository,
                          StoreRepository storeRepository,
                          ProductRepository productRepository) {
        this.storagePlaceRepository = storagePlaceRepository;
        this.unitRepository = unitRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
    }

    @CacheEvict(value = "catalog-refs", allEntries = true)
    public CatalogDtos.StoragePlaceResponse createStoragePlace(CatalogDtos.StoragePlaceRequest request) {
        StoragePlace storagePlace = new StoragePlace();
        mapStoragePlace(request, storagePlace);
        return toStoragePlaceResponse(storagePlaceRepository.save(storagePlace));
    }

    @Cacheable("catalog-refs")
    @Transactional(readOnly = true)
    public Page<CatalogDtos.StoragePlaceResponse> listStoragePlaces(Pageable pageable) {
        return storagePlaceRepository.findAll(pageable).map(this::toStoragePlaceResponse);
    }

    @CacheEvict(value = "catalog-refs", allEntries = true)
    public CatalogDtos.UnitResponse createUnit(CatalogDtos.UnitRequest request) {
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setCode(request.code());
        unit.setName(request.name());
        unit.setShortName(request.shortName());
        unit.setUnitType(request.unitType());
        unit.setActive(request.active());
        return toUnitResponse(unitRepository.save(unit));
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.UnitResponse> listUnits(Pageable pageable) {
        return unitRepository.findAll(pageable).map(this::toUnitResponse);
    }

    @CacheEvict(value = "catalog-refs", allEntries = true)
    public CatalogDtos.CategoryResponse createCategory(CatalogDtos.CategoryRequest request) {
        Category category = new Category();
        mapCategory(request, category);
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.CategoryResponse> listCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::toCategoryResponse);
    }

    @CacheEvict(value = "catalog-refs", allEntries = true)
    public CatalogDtos.StoreResponse createStore(CatalogDtos.StoreRequest request) {
        Store store = new Store();
        mapStore(request, store);
        return toStoreResponse(storeRepository.save(store));
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.StoreResponse> listStores(Pageable pageable) {
        return storeRepository.findAll(pageable).map(this::toStoreResponse);
    }

    public CatalogDtos.ProductResponse createProduct(CatalogDtos.ProductRequest request) {
        Product product = new Product();
        mapProduct(request, product);
        return toProductResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public Page<CatalogDtos.ProductResponse> listProducts(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return productRepository.findAll(pageable).map(this::toProductResponse);
        }
        return productRepository.findByNameContainingIgnoreCase(query, pageable).map(this::toProductResponse);
    }

    private void mapStoragePlace(CatalogDtos.StoragePlaceRequest request, StoragePlace storagePlace) {
        storagePlace.setName(request.name());
        storagePlace.setDescription(request.description());
        storagePlace.setIcon(request.icon());
        storagePlace.setColor(request.color());
        storagePlace.setSortOrder(request.sortOrder());
        storagePlace.setActive(request.active());
    }

    private void mapCategory(CatalogDtos.CategoryRequest request, Category category) {
        category.setName(request.name());
        category.setDescription(request.description());
        category.setIcon(request.icon());
        category.setColor(request.color());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.active());
    }

    private void mapStore(CatalogDtos.StoreRequest request, Store store) {
        store.setName(request.name());
        store.setDescription(request.description());
        store.setIcon(request.icon());
        store.setColor(request.color());
        store.setSortOrder(request.sortOrder());
        store.setActive(request.active());
    }

    private void mapProduct(CatalogDtos.ProductRequest request, Product product) {
        product.setName(request.name());
        product.setCategory(categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found")));
        product.setDefaultUnit(unitRepository.findById(request.defaultUnitId())
                .orElseThrow(() -> new EntityNotFoundException("Default unit not found")));
        product.setDefaultStoragePlace(storagePlaceRepository.findById(request.defaultStoragePlaceId())
                .orElseThrow(() -> new EntityNotFoundException("Default storage place not found")));
        product.setDefaultStore(request.defaultStoreId() == null ? null : storeRepository.findById(request.defaultStoreId())
                .orElseThrow(() -> new EntityNotFoundException("Default store not found")));
        product.setPhotoUrl(request.photoUrl());
        product.setDescription(request.description());
        product.setShelfLifeDays(request.shelfLifeDays());
        product.setMinimumQuantityThreshold(request.minimumQuantityThreshold());
        product.setReorderQuantity(request.reorderQuantity());
        product.setAutoAddToShoppingList(request.autoAddToShoppingList());
        product.setBarcode(request.barcode());
        product.setNote(request.note());
        product.setActive(request.active());
    }

    private CatalogDtos.StoragePlaceResponse toStoragePlaceResponse(StoragePlace storagePlace) {
        return new CatalogDtos.StoragePlaceResponse(
                storagePlace.getId(), storagePlace.getName(), storagePlace.getDescription(), storagePlace.getIcon(),
                storagePlace.getColor(), storagePlace.getSortOrder(), storagePlace.isActive(),
                storagePlace.getVersion(), storagePlace.getCreatedAt(), storagePlace.getUpdatedAt());
    }

    private CatalogDtos.UnitResponse toUnitResponse(UnitOfMeasure unit) {
        return new CatalogDtos.UnitResponse(
                unit.getId(), unit.getCode(), unit.getName(), unit.getShortName(), unit.getUnitType(), unit.isActive(),
                unit.getVersion(), unit.getCreatedAt(), unit.getUpdatedAt());
    }

    private CatalogDtos.CategoryResponse toCategoryResponse(Category category) {
        return new CatalogDtos.CategoryResponse(
                category.getId(), category.getName(), category.getDescription(), category.getIcon(), category.getColor(),
                category.getSortOrder(), category.isActive(), category.getVersion(), category.getCreatedAt(), category.getUpdatedAt());
    }

    private CatalogDtos.StoreResponse toStoreResponse(Store store) {
        return new CatalogDtos.StoreResponse(
                store.getId(), store.getName(), store.getDescription(), store.getIcon(), store.getColor(),
                store.getSortOrder(), store.isActive(), store.getVersion(), store.getCreatedAt(), store.getUpdatedAt());
    }

    private CatalogDtos.ProductResponse toProductResponse(Product product) {
        UUID defaultStoreId = product.getDefaultStore() == null ? null : product.getDefaultStore().getId();
        return new CatalogDtos.ProductResponse(
                product.getId(), product.getName(), product.getCategory().getId(), product.getDefaultUnit().getId(),
                product.getDefaultStoragePlace().getId(), defaultStoreId,
                product.getPhotoUrl(), product.getDescription(), product.getShelfLifeDays(),
                product.getMinimumQuantityThreshold(), product.getReorderQuantity(),
                product.isAutoAddToShoppingList(), product.getBarcode(), product.getNote(), product.isActive(),
                product.getVersion(), product.getCreatedAt(), product.getUpdatedAt());
    }
}
