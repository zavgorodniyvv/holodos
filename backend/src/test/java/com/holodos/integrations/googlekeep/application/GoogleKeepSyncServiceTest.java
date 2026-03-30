package com.holodos.integrations.googlekeep.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.integrations.googlekeep.domain.SyncBinding;
import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
import com.holodos.integrations.googlekeep.infrastructure.SyncEventRepository;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GoogleKeepSyncServiceTest {
    @Mock SyncBindingRepository syncBindingRepository;
    @Mock SyncEventRepository syncEventRepository;
    @Mock ShoppingListItemRepository shoppingListItemRepository;
    @Mock GoogleKeepClient googleKeepClient;

    GoogleKeepSyncService service;

    @BeforeEach
    void setUp() {
        service = new GoogleKeepSyncService(syncBindingRepository, syncEventRepository, shoppingListItemRepository, googleKeepClient);
    }

    @Test
    void syncNowProcessesInboundAndPushesOutbound() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("default");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(true);

        ShoppingListItem item = new ShoppingListItem();
        item.setTitle("Milk");
        item.setStatus(ShoppingItemStatus.ACTIVE);
        ReflectionTestUtils.setField(item, "id", 10L);

        when(syncBindingRepository.findByUserKeyAndProvider("default", GoogleKeepSyncService.PROVIDER)).thenReturn(Optional.of(b));
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE)).thenReturn(List.of(item));
        when(shoppingListItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(googleKeepClient.fetchChecklist("note-1"))
            .thenReturn(new GoogleKeepClient.KeepRemoteState("etag-1",
                List.of(new GoogleKeepClient.KeepChecklistItem("Milk", true, "shopping-10"))));
        when(googleKeepClient.pushChecklist(any(), any(), any())).thenReturn(new GoogleKeepClient.KeepSyncResult(true, "etag-2", "ok"));
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String res = service.syncNow("default");

        assertEquals("ok", res);
        verify(syncEventRepository, times(1)).save(any());
        verify(syncBindingRepository, atLeastOnce()).save(any());
        verify(shoppingListItemRepository).save(item);
    }

    // --- bind ---

    @Test
    void bind_createsNewBinding() {
        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.empty());
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SyncBinding result = service.bind("user1", "note-abc");

        assertEquals("user1", result.getUserKey());
        assertEquals("note-abc", result.getRemoteNoteId());
        assertTrue(result.isEnabled());
        verify(syncBindingRepository).save(any(SyncBinding.class));
    }

    @Test
    void bind_updatesExistingBinding() {
        SyncBinding existing = new SyncBinding();
        existing.setUserKey("user1");
        existing.setProvider(GoogleKeepSyncService.PROVIDER);
        existing.setRemoteNoteId("old-note");
        existing.setEnabled(true);

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(existing));
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SyncBinding result = service.bind("user1", "new-note");

        assertEquals("new-note", result.getRemoteNoteId());
        verify(syncBindingRepository).save(existing);
    }

    // --- listBindings ---

    @Test
    void listBindings_returnsAll() {
        SyncBinding b1 = new SyncBinding();
        b1.setUserKey("user1");
        SyncBinding b2 = new SyncBinding();
        b2.setUserKey("user2");

        when(syncBindingRepository.findAll()).thenReturn(List.of(b1, b2));

        List<SyncBinding> result = service.listBindings();

        assertEquals(2, result.size());
        verify(syncBindingRepository).findAll();
    }

    // --- syncNow guard cases ---

    @Test
    void syncNow_throwsWhenBindingNotFound() {
        when(syncBindingRepository.findByUserKeyAndProvider("missing", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.syncNow("missing"));
    }

    @Test
    void syncNow_throwsWhenDisabled() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("user1");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(false);

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(b));

        assertThrows(IllegalArgumentException.class, () -> service.syncNow("user1"));
    }

    @Test
    void syncNow_throwsWhenRetryWindowActive() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("user1");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(true);
        b.setNextRetryAt(OffsetDateTime.now().plusHours(1));

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(b));

        assertThrows(IllegalStateException.class, () -> service.syncNow("user1"));
    }

    // --- syncNow failure handling ---

    @Test
    void syncNow_handlesFailedPushResult() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("user1");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(true);

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(b));
        when(googleKeepClient.fetchChecklist("note-1"))
            .thenReturn(new GoogleKeepClient.KeepRemoteState("etag-1", List.of()));
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE))
            .thenReturn(List.of());
        when(googleKeepClient.pushChecklist(any(), any(), any()))
            .thenReturn(new GoogleKeepClient.KeepSyncResult(false, "etag-1", "conflict"));
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = service.syncNow("user1");

        assertEquals("conflict", result);
        assertEquals(1, b.getFailureCount());
        assertEquals("FAILED", b.getLastSyncStatus());
        assertNotNull(b.getNextRetryAt());
        verify(syncBindingRepository, atLeastOnce()).save(b);
    }

    @Test
    void syncNow_throwsOnFetchException() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("user1");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(true);

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(b));
        when(googleKeepClient.fetchChecklist("note-1"))
            .thenThrow(new IllegalStateException("network error"));
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(IllegalStateException.class, () -> service.syncNow("user1"));
        assertEquals(1, b.getFailureCount());
        assertEquals("FAILED", b.getLastSyncStatus());
    }

    // --- syncNow item-level state transitions ---

    @Test
    void syncNow_revertsCompletedItemToActiveWhenUncheckedInKeep() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("user1");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(true);

        ShoppingListItem item = new ShoppingListItem();
        item.setTitle("Butter");
        item.setStatus(ShoppingItemStatus.COMPLETED);
        item.setCompletedAt(OffsetDateTime.now().minusMinutes(10));
        ReflectionTestUtils.setField(item, "id", 5L);

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(b));
        when(googleKeepClient.fetchChecklist("note-1"))
            .thenReturn(new GoogleKeepClient.KeepRemoteState("etag-1",
                List.of(new GoogleKeepClient.KeepChecklistItem("Butter", false, "shopping-5"))));
        when(shoppingListItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE))
            .thenReturn(List.of());
        when(googleKeepClient.pushChecklist(any(), any(), any()))
            .thenReturn(new GoogleKeepClient.KeepSyncResult(true, "etag-2", "ok"));
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.syncNow("user1");

        assertEquals(ShoppingItemStatus.ACTIVE, item.getStatus());
        assertNull(item.getCompletedAt());
        verify(shoppingListItemRepository).save(item);
    }

    @Test
    void syncNow_createsNewItemFromKeep() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("user1");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(true);

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(b));
        when(googleKeepClient.fetchChecklist("note-1"))
            .thenReturn(new GoogleKeepClient.KeepRemoteState("etag-1",
                List.of(new GoogleKeepClient.KeepChecklistItem("Eggs", false, null))));
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE))
            .thenReturn(List.of());
        when(googleKeepClient.pushChecklist(any(), any(), any()))
            .thenReturn(new GoogleKeepClient.KeepSyncResult(true, "etag-2", "ok"));
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.syncNow("user1");

        ArgumentCaptor<ShoppingListItem> captor = ArgumentCaptor.forClass(ShoppingListItem.class);
        verify(shoppingListItemRepository).save(captor.capture());
        ShoppingListItem created = captor.getValue();
        assertEquals("Eggs", created.getTitle());
        assertEquals(ShoppingItemSource.KEEP_SYNC, created.getSource());
        assertEquals(ShoppingItemStatus.ACTIVE, created.getStatus());
    }

    // --- syncNow success resets failure state ---

    @Test
    void syncNow_successResetsFailureState() {
        SyncBinding b = new SyncBinding();
        b.setUserKey("user1");
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(true);
        b.setFailureCount(3);
        b.setLastErrorMessage("previous error");
        b.setNextRetryAt(OffsetDateTime.now().minusMinutes(1));
        b.setLastSyncStatus("FAILED");

        when(syncBindingRepository.findByUserKeyAndProvider("user1", GoogleKeepSyncService.PROVIDER))
            .thenReturn(Optional.of(b));
        when(googleKeepClient.fetchChecklist("note-1"))
            .thenReturn(new GoogleKeepClient.KeepRemoteState("etag-1", List.of()));
        when(shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE))
            .thenReturn(List.of());
        when(googleKeepClient.pushChecklist(any(), any(), any()))
            .thenReturn(new GoogleKeepClient.KeepSyncResult(true, "etag-2", "ok"));
        when(syncBindingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.syncNow("user1");

        assertEquals(0, b.getFailureCount());
        assertNull(b.getLastErrorMessage());
        assertNull(b.getNextRetryAt());
        assertEquals("SUCCESS", b.getLastSyncStatus());
    }
}
