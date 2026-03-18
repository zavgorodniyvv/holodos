package com.holodos.catalog.application;

import com.holodos.common.domain.NotFoundException;
import com.holodos.catalog.api.CatalogDtos.ProductResponse;
import com.holodos.catalog.api.CatalogDtos.ProductUpsertRequest;
import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.Store;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.CategoryRepository;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.StoreRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final StoreRepository storeRepository;

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        UnitRepository unitRepository,
        StoragePlaceRepository storagePlaceRepository,
        StoreRepository storeRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.storagePlaceRepository = storagePlaceRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String search, Pageable pageable) {
        Page<Product> page = (search == null || search.isBlank())
            ? productRepository.findAll(pageable)
            : productRepository.findByNameContainingIgnoreCase(search, pageable);
        return page.map(this::map);
    }

    public ProductResponse create(ProductUpsertRequest request) {
        Product product = new Product();
        apply(product, request);
        return map(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductUpsertRequest request) {
        Product product = productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
        apply(product, request);
        return map(productRepository.save(product));
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Product not found");
        }
        productRepository.deleteById(id);
    }

    private void apply(Product product, ProductUpsertRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new NotFoundException("Category not found"));
        UnitOfMeasure unit = unitRepository.findById(request.defaultUnitId())
            .orElseThrow(() -> new NotFoundException("Unit not found"));
        StoragePlace storagePlace = storagePlaceRepository.findById(request.defaultStoragePlaceId())
            .orElseThrow(() -> new NotFoundException("Storage place not found"));

        Store store = null;
        if (request.defaultStoreId() != null) {
            store = storeRepository.findById(request.defaultStoreId())
                .orElseThrow(() -> new NotFoundException("Store not found"));
        }

        product.setName(request.name());
        product.setCategory(category);
        product.setDefaultUnit(unit);
        product.setDefaultStoragePlace(storagePlace);
        product.setDefaultStore(store);
        product.setPhotoKey(request.photoKey());
        product.setDescription(request.description());
        product.setShelfLifeDays(request.shelfLifeDays());
        product.setMinimumQuantityThreshold(request.minimumQuantityThreshold());
        product.setReorderQuantity(request.reorderQuantity());
        product.setAutoAddShopping(request.autoAddShopping());
        product.setBarcode(request.barcode());
        product.setNote(request.note());
        product.setActive(request.active());
    }

    private ProductResponse map(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getDefaultUnit().getId(),
            product.getDefaultUnit().getName(),
            product.getDefaultStoragePlace().getId(),
            product.getDefaultStoragePlace().getName(),
            product.getDefaultStore() == null ? null : product.getDefaultStore().getId(),
            product.getDefaultStore() == null ? null : product.getDefaultStore().getName(),
            product.getPhotoKey(),
            product.getDescription(),
            product.getShelfLifeDays(),
            product.getMinimumQuantityThreshold(),
            product.getReorderQuantity(),
            product.isAutoAddShopping(),
            product.getBarcode(),
            product.getNote(),
            product.isActive(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getVersion()
        );
    }
}
