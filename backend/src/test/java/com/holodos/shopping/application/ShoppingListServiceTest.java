package com.holodos.shopping.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.holodos.catalog.domain.Product;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoreRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.DomainEventPublisher;
import com.holodos.common.application.events.OperationLogEvent;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemUpsertRequest;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {
    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock ProductRepository productRepository;
    @Mock UnitRepository unitRepository;
    @Mock StoreRepository storeRepository;
    @Mock DomainEventPublisher domainEventPublisher;

    ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        shoppingListService = new ShoppingListService(shoppingListItemRepository, productRepository, unitRepository, storeRepository, domainEventPublisher);
    }

    @Test
    void createWritesOperationLog() {
        when(shoppingListItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        shoppingListService.create(new ShoppingItemUpsertRequest(null, "Bread", BigDecimal.ONE, null, null, ShoppingItemSource.MANUAL, null, 0));
        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertEquals("SHOPPING_CREATE", eventCaptor.getValue().eventType());
    }

    @Test
    void autoAddLeavesExistingActiveItemUntouched() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 42L);
        product.setAutoAddShopping(true);
        ShoppingListItem existing = new ShoppingListItem();
        existing.setQuantity(BigDecimal.ONE);
        when(shoppingListItemRepository.findFirstByProductIdAndStatus(42L, ShoppingItemStatus.ACTIVE))
            .thenReturn(java.util.Optional.of(existing));

        shoppingListService.autoAddIfMissing(product, BigDecimal.valueOf(2));
        shoppingListService.autoAddIfMissing(product, BigDecimal.valueOf(2));

        assertEquals(BigDecimal.ONE, existing.getQuantity());
        verify(shoppingListItemRepository, never()).save(any());
        verify(domainEventPublisher, never()).publish(any());
    }

    @Test
    void autoAddCreatesItemWhenNoneActive() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 42L);
        product.setName("Milk");
        product.setAutoAddShopping(true);
        when(shoppingListItemRepository.findFirstByProductIdAndStatus(42L, ShoppingItemStatus.ACTIVE))
            .thenReturn(java.util.Optional.empty());
        when(shoppingListItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        shoppingListService.autoAddIfMissing(product, BigDecimal.valueOf(2));

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).save(captor.capture());
        assertEquals("Milk", captor.getValue().getTitle());
        assertEquals(BigDecimal.valueOf(2), captor.getValue().getQuantity());
        assertEquals(ShoppingItemSource.AUTO_REPLENISHMENT, captor.getValue().getSource());
    }
}
