package com.holodos.shopping.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoreRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.OperationLogService;
import com.holodos.shopping.api.ShoppingDtos.ShoppingItemUpsertRequest;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {
    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock ProductRepository productRepository;
    @Mock UnitRepository unitRepository;
    @Mock StoreRepository storeRepository;
    @Mock OperationLogService operationLogService;

    ShoppingListService shoppingListService;

    @BeforeEach
    void setUp() {
        shoppingListService = new ShoppingListService(shoppingListItemRepository, productRepository, unitRepository, storeRepository, operationLogService);
    }

    @Test
    void createWritesOperationLog() {
        when(shoppingListItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        shoppingListService.create(new ShoppingItemUpsertRequest(null, "Bread", BigDecimal.ONE, null, null, ShoppingItemSource.MANUAL, null, 0));
        verify(operationLogService).log(eq("SHOPPING_CREATE"), eq("ShoppingListItem"), anyString(), anyMap());
    }
}
