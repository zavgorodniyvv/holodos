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
    void autoAddMergesExistingActiveItem() {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 42L);
        product.setAutoAddShopping(true);
        ShoppingListItem existing = new ShoppingListItem();
        existing.setQuantity(BigDecimal.ONE);
        when(shoppingListItemRepository.findFirstByProductIdAndStatus(42L, ShoppingItemStatus.ACTIVE))
            .thenReturn(java.util.Optional.of(existing));
        when(shoppingListItemRepository.save(existing)).thenAnswer(i -> i.getArgument(0));

        shoppingListService.autoAddIfMissing(product, BigDecimal.valueOf(2));

        assertEquals(BigDecimal.valueOf(3), existing.getQuantity());
        verify(shoppingListItemRepository).save(existing);
        ArgumentCaptor<OperationLogEvent> eventCaptor = ArgumentCaptor.forClass(OperationLogEvent.class);
        verify(domainEventPublisher, atLeastOnce()).publish(eventCaptor.capture());
        OperationLogEvent lastEvent = eventCaptor.getValue();
        assertEquals("SHOPPING_AUTO_ADD", lastEvent.eventType());
        assertEquals(true, lastEvent.payload().get("merged"));
    }
}
