package com.holodos.catalog.application;

import com.holodos.common.domain.NotFoundException;
import com.holodos.catalog.api.CatalogDtos.DictionaryResponse;
import com.holodos.catalog.api.CatalogDtos.DictionaryUpsertRequest;
import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.Store;
import com.holodos.catalog.infrastructure.CategoryRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.StoreRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CatalogDictionaryService {

    private final StoragePlaceRepository storagePlaceRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    public CatalogDictionaryService(
        StoragePlaceRepository storagePlaceRepository,
        CategoryRepository categoryRepository,
        StoreRepository storeRepository
    ) {
        this.storagePlaceRepository = storagePlaceRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<DictionaryResponse> listStoragePlaces() {
        return storagePlaceRepository.findAll().stream().map(this::map).toList();
    }

    public DictionaryResponse createStoragePlace(DictionaryUpsertRequest request) {
        StoragePlace entity = new StoragePlace();
        apply(entity, request);
        return map(storagePlaceRepository.save(entity));
    }

    public DictionaryResponse updateStoragePlace(Long id, DictionaryUpsertRequest request) {
        StoragePlace entity = storagePlaceRepository.findById(id).orElseThrow(() -> new NotFoundException("Storage place not found"));
        apply(entity, request);
        return map(storagePlaceRepository.save(entity));
    }

    public void deleteStoragePlace(Long id) {
        if (!storagePlaceRepository.existsById(id)) {
            throw new NotFoundException("Storage place not found");
        }
        storagePlaceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<DictionaryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(this::map).toList();
    }

    public DictionaryResponse createCategory(DictionaryUpsertRequest request) {
        Category entity = new Category();
        apply(entity, request);
        return map(categoryRepository.save(entity));
    }

    public DictionaryResponse updateCategory(Long id, DictionaryUpsertRequest request) {
        Category entity = categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        apply(entity, request);
        return map(categoryRepository.save(entity));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<DictionaryResponse> listStores() {
        return storeRepository.findAll().stream().map(this::map).toList();
    }

    public DictionaryResponse createStore(DictionaryUpsertRequest request) {
        Store entity = new Store();
        apply(entity, request);
        return map(storeRepository.save(entity));
    }

    public DictionaryResponse updateStore(Long id, DictionaryUpsertRequest request) {
        Store entity = storeRepository.findById(id).orElseThrow(() -> new NotFoundException("Store not found"));
        apply(entity, request);
        return map(storeRepository.save(entity));
    }

    public void deleteStore(Long id) {
        if (!storeRepository.existsById(id)) {
            throw new NotFoundException("Store not found");
        }
        storeRepository.deleteById(id);
    }

    private void apply(StoragePlace entity, DictionaryUpsertRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setIcon(request.icon());
        entity.setColor(request.color());
        entity.setSortOrder(request.sortOrder());
        entity.setActive(request.active());
    }

    private void apply(Category entity, DictionaryUpsertRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setIcon(request.icon());
        entity.setColor(request.color());
        entity.setSortOrder(request.sortOrder());
        entity.setActive(request.active());
    }

    private void apply(Store entity, DictionaryUpsertRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setIcon(request.icon());
        entity.setColor(request.color());
        entity.setSortOrder(request.sortOrder());
        entity.setActive(request.active());
    }

    private DictionaryResponse map(StoragePlace entity) {
        return new DictionaryResponse(entity.getId(), entity.getName(), entity.getDescription(), entity.getIcon(), entity.getColor(),
            entity.getSortOrder(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }

    private DictionaryResponse map(Category entity) {
        return new DictionaryResponse(entity.getId(), entity.getName(), entity.getDescription(), entity.getIcon(), entity.getColor(),
            entity.getSortOrder(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }

    private DictionaryResponse map(Store entity) {
        return new DictionaryResponse(entity.getId(), entity.getName(), entity.getDescription(), entity.getIcon(), entity.getColor(),
            entity.getSortOrder(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getVersion());
    }
}
