package com.holodos.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.holodos.catalog.api.CatalogDtos;
import com.holodos.catalog.domain.model.Category;
import com.holodos.catalog.domain.model.Product;
import com.holodos.catalog.domain.model.StoragePlace;
import com.holodos.catalog.domain.model.UnitOfMeasure;
import com.holodos.catalog.domain.model.UnitType;
import com.holodos.catalog.domain.repository.CategoryRepository;
import com.holodos.catalog.domain.repository.ProductRepository;
import com.holodos.catalog.domain.repository.StoragePlaceRepository;
import com.holodos.catalog.domain.repository.StoreRepository;
import com.holodos.catalog.domain.repository.UnitRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private StoragePlaceRepository storagePlaceRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ProductRepository productRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(storagePlaceRepository, unitRepository, categoryRepository, storeRepository, productRepository);
    }

    @Test
    void shouldCreateProductWhenDependenciesExist() {
        UUID categoryId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID storagePlaceId = UUID.randomUUID();

        Category category = new Category();
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setUnitType(UnitType.COUNT);
        StoragePlace storagePlace = new StoragePlace();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(storagePlaceRepository.findById(storagePlaceId)).thenReturn(Optional.of(storagePlace));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogDtos.ProductRequest request = new CatalogDtos.ProductRequest(
                "Milk", categoryId, unitId, storagePlaceId, null, null,
                null, 10, null, null, true, null, null, true);

        CatalogDtos.ProductResponse response = catalogService.createProduct(request);

        assertEquals("Milk", response.name());
        assertEquals(10, response.shelfLifeDays());
    }

    @Test
    void shouldFailWhenCategoryNotFound() {
        UUID missingCategoryId = UUID.randomUUID();
        CatalogDtos.ProductRequest request = new CatalogDtos.ProductRequest(
                "Milk", missingCategoryId, UUID.randomUUID(), UUID.randomUUID(), null,
                null, null, null, null, null, true, null, null, true);

        when(categoryRepository.findById(missingCategoryId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.createProduct(request));
    }
}
