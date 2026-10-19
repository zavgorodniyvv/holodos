package com.holodos.integrations.googletasks.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.common.domain.ConflictException;
import com.holodos.common.domain.ExternalServiceException;
import com.holodos.integrations.googletasks.application.GoogleTasksClient.RemoteTask;
import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksBindingRepository;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class GoogleTasksSyncServiceTest {
    @Mock GoogleTasksBindingRepository bindingRepository;
    @Mock GoogleTasksClient client;
    @Mock GoogleTasksOAuthService oAuthService;
    @Mock ShoppingListItemRepository shoppingListItemRepository;

    GoogleTasksSyncService service;
    GoogleTasksBinding binding;

    @BeforeEach
    void setUp() {
        service = new GoogleTasksSyncService(bindingRepository, client, oAuthService, shoppingListItemRepository,
            TransactionOperations.withoutTransaction());
        binding = new GoogleTasksBinding();
        binding.setUserKey("slava");
        binding.setEnabled(true);
        binding.setTaskListId("list-1");
        when(bindingRepository.findByUserKey("slava")).thenReturn(Optional.of(binding));
    }

    @Test
    void failureIsRecordedAndReportedAsExternalError() {
        when(oAuthService.getValidAccessToken(binding)).thenThrow(new IllegalStateException("token revoked"));

        assertThrows(ExternalServiceException.class, () -> service.syncNow("slava"));

        assertEquals(1, binding.getFailureCount());
        assertEquals("FAILED", binding.getLastSyncStatus());
        assertEquals("token revoked", binding.getLastErrorMessage());
        assertNotNull(binding.getNextRetryAt());
        verify(bindingRepository).save(binding);
    }

    @Test
    void duplicateRemoteTasksDoNotBreakSync() {
        ShoppingListItem item = new ShoppingListItem();
        item.setTitle("Milk");
        item.setStatus(ShoppingItemStatus.ACTIVE);
        ReflectionTestUtils.setField(item, "id", 10L);
        when(oAuthService.getValidAccessToken(binding)).thenReturn("token");
        when(client.listTasks("list-1", "token")).thenReturn(List.of(
            new RemoteTask("t1", "Milk", "shopping-10", false),
            new RemoteTask("t2", "Milk", "shopping-10", false)));
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE)).thenReturn(List.of(item));
        when(shoppingListItemRepository.findById(10L)).thenReturn(Optional.of(item));

        String result = service.syncNow("slava");

        assertEquals("Sync completed successfully", result);
        assertEquals("SUCCESS", binding.getLastSyncStatus());
        verify(client, never()).createTask(any(), any(), any(), any());
    }

    @Test
    void concurrentSyncForSameUserIsRejected() throws Exception {
        CountDownLatch insideSync = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(oAuthService.getValidAccessToken(binding)).thenAnswer(inv -> {
            insideSync.countDown();
            release.await(5, TimeUnit.SECONDS);
            return "token";
        });
        when(client.listTasks("list-1", "token")).thenReturn(List.of());
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE)).thenReturn(List.of());

        Thread first = new Thread(() -> service.syncNow("slava"));
        first.start();
        try {
            insideSync.await(5, TimeUnit.SECONDS);
            assertThrows(ConflictException.class, () -> service.syncNow("slava"));
        } finally {
            release.countDown();
            first.join(5000);
        }
    }
}
