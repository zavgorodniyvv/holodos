package com.holodos.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.holodos.catalog.api.CatalogDtos.ProductSuggestionResponse;
import com.holodos.catalog.application.ProductClassifier.Classification;
import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.CategoryRepository;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.DomainEventPublisher;
import com.holodos.common.application.events.OperationLogEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductResolverTest {
    @Mock ProductRepository productRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UnitRepository unitRepository;
    @Mock StoragePlaceRepository storagePlaceRepository;
    @Mock ProductClassifier productClassifier;
    @Mock DomainEventPublisher domainEventPublisher;

    ProductResolver resolver;

    Category food = category(1L, "Food", 10);
    Category household = category(2L, "Household", 20);
    UnitOfMeasure liter = unit(4L, "l");
    UnitOfMeasure piece = unit(1L, "piece");
    StoragePlace fridge = place(1L, "Fridge", 10);
    StoragePlace pantry = place(3L, "Pantry", 30);

    @BeforeEach
    void setUp() {
        resolver = new ProductResolver(productRepository, categoryRepository, unitRepository, storagePlaceRepository,
            productClassifier, domainEventPublisher);
        when(categoryRepository.findAll()).thenReturn(List.of(household, food));
        when(unitRepository.findAll()).thenReturn(List.of(liter, piece));
        when(storagePlaceRepository.findAll()).thenReturn(List.of(pantry, fridge));
        when(productRepository.findFirstByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void suggestReturnsExistingProductWithoutCallingAi() {
        Product milk = new Product();
        ReflectionTestUtils.setField(milk, "id", 42L);
        milk.setName("Молоко");
        milk.setCategory(food);
        milk.setDefaultUnit(liter);
        milk.setDefaultStoragePlace(fridge);
        when(productRepository.findFirstByNameIgnoreCase("Молоко")).thenReturn(Optional.of(milk));

        ProductSuggestionResponse s = resolver.suggest("  молоко ");

        assertEquals(42L, s.existingProductId());
        assertEquals(4L, s.unitId());
        verifyNoInteractions(productClassifier);
    }

    @Test
    void suggestUsesAiAndFallsBackForUnknownIds() {
        when(productClassifier.classify(eq("Молоко"), anyList(), anyList(), anyList()))
            .thenReturn(Optional.of(new Classification(1L, 4L, 999L, 7)));

        ProductSuggestionResponse s = resolver.suggest("молоко");

        assertNull(s.existingProductId());
        assertEquals("Молоко", s.name());
        assertEquals(1L, s.categoryId());
        assertEquals(4L, s.unitId());
        assertEquals(1L, s.storagePlaceId(), "unknown place id falls back to the first place by sort order");
        assertEquals(7, s.shelfLifeDays());
        assertTrue(s.aiUsed());
    }

    @Test
    void suggestWithoutAiUsesDefaults() {
        when(productClassifier.classify(anyString(), anyList(), anyList(), anyList())).thenReturn(Optional.empty());

        ProductSuggestionResponse s = resolver.suggest("гвозди");

        assertEquals(1L, s.categoryId());
        assertEquals(1L, s.unitId(), "piece is the default unit");
        assertEquals(1L, s.storagePlaceId());
        assertNull(s.shelfLifeDays());
        assertFalse(s.aiUsed());
    }

    @Test
    void resolveOrCreateKeepsCallerValuesAndSkipsAiWhenComplete() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(household));
        when(unitRepository.findById(1L)).thenReturn(Optional.of(piece));
        when(storagePlaceRepository.findById(3L)).thenReturn(Optional.of(pantry));

        Product created = resolver.resolveOrCreate("губки", 2L, 1L, 3L, null);

        assertEquals("Губки", created.getName());
        assertSame(household, created.getCategory());
        assertSame(piece, created.getDefaultUnit());
        assertSame(pantry, created.getDefaultStoragePlace());
        assertFalse(created.isAutoAddShopping());
        verifyNoInteractions(productClassifier);
        ArgumentCaptor<OperationLogEvent> event = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(domainEventPublisher).publish(event.capture());
        assertEquals("PRODUCT_AUTO_CREATE", event.getValue().eventType());
    }

    @Test
    void resolveOrCreateFillsOnlyMissingValuesFromAi() {
        when(storagePlaceRepository.findById(3L)).thenReturn(Optional.of(pantry));
        when(productClassifier.classify(anyString(), anyList(), anyList(), anyList()))
            .thenReturn(Optional.of(new Classification(1L, 4L, 1L, 5)));

        Product created = resolver.resolveOrCreate("молоко", null, null, 3L, null);

        assertSame(food, created.getCategory());
        assertSame(liter, created.getDefaultUnit());
        assertSame(pantry, created.getDefaultStoragePlace(), "caller's place wins over the AI");
        assertEquals(5, created.getShelfLifeDays());
    }

    @Test
    void resolveOrCreateReturnsExistingProduct() {
        Product existing = new Product();
        when(productRepository.findFirstByNameIgnoreCase("Хлеб")).thenReturn(Optional.of(existing));

        assertSame(existing, resolver.resolveOrCreate("хлеб", null, null, null, null));
        verify(productRepository, never()).save(any());
        verifyNoInteractions(productClassifier, domainEventPublisher);
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> resolver.suggest("   "));
    }

    private static Category category(Long id, String name, int sortOrder) {
        Category c = new Category();
        ReflectionTestUtils.setField(c, "id", id);
        c.setName(name);
        c.setSortOrder(sortOrder);
        return c;
    }

    private static UnitOfMeasure unit(Long id, String code) {
        UnitOfMeasure u = new UnitOfMeasure();
        ReflectionTestUtils.setField(u, "id", id);
        u.setCode(code);
        u.setName(code);
        u.setShortName(code);
        return u;
    }

    private static StoragePlace place(Long id, String name, int sortOrder) {
        StoragePlace p = new StoragePlace();
        ReflectionTestUtils.setField(p, "id", id);
        p.setName(name);
        p.setSortOrder(sortOrder);
        return p;
    }
}
