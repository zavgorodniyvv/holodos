package com.holodos.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.holodos.catalog.api.CatalogDtos.ProductUpsertRequest;
import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.CategoryRepository;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.StoreRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private StoragePlaceRepository storagePlaceRepository;
    @Mock
    private StoreRepository storeRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, categoryRepository, unitRepository, storagePlaceRepository, storeRepository);
    }

    @Test
    void createFailsWhenCategoryMissing() {
        ProductUpsertRequest request = baseRequest();
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> productService.create(request));
    }

    @Test
    void createPersistsProductWhenReferencesExist() {
        ProductUpsertRequest request = baseRequest();
        Category category = new Category();
        category.setName("Food");
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setName("Piece");
        StoragePlace storagePlace = new StoragePlace();
        storagePlace.setName("Fridge");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(unit));
        when(storagePlaceRepository.findById(1L)).thenReturn(Optional.of(storagePlace));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productService.create(request);

        assertEquals("Milk", response.name());
        assertEquals("Food", response.categoryName());
        assertEquals("Fridge", response.defaultStoragePlaceName());
    }

    private ProductUpsertRequest baseRequest() {
        return new ProductUpsertRequest(
            "Milk",
            1L,
            1L,
            1L,
            null,
            null,
            "desc",
            7,
            null,
            null,
            true,
            null,
            null,
            true
        );
    }
}
