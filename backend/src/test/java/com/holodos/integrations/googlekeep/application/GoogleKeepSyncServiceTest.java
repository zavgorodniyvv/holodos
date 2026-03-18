package com.holodos.integrations.googlekeep.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.integrations.googlekeep.domain.SyncBinding;
import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
import com.holodos.integrations.googlekeep.infrastructure.SyncEventRepository;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
