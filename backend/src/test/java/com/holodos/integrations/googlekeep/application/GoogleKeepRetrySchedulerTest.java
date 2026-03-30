package com.holodos.integrations.googlekeep.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.holodos.integrations.googlekeep.domain.SyncBinding;
import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleKeepRetrySchedulerTest {

    @Mock SyncBindingRepository syncBindingRepository;
    @Mock GoogleKeepSyncService googleKeepSyncService;

    GoogleKeepRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new GoogleKeepRetryScheduler(syncBindingRepository, googleKeepSyncService);
    }

    @Test
    void retryFailedSyncs_callsSyncForEligibleBinding() {
        SyncBinding b = binding("user1", true, null);
        when(syncBindingRepository.findAll()).thenReturn(List.of(b));

        scheduler.retryFailedSyncs();

        verify(googleKeepSyncService).syncNow("user1");
    }

    @Test
    void retryFailedSyncs_skipsDisabledBindings() {
        SyncBinding b = binding("user1", false, null);
        when(syncBindingRepository.findAll()).thenReturn(List.of(b));

        scheduler.retryFailedSyncs();

        verifyNoInteractions(googleKeepSyncService);
    }

    @Test
    void retryFailedSyncs_skipsBindingsWithFutureRetryAt() {
        SyncBinding b = binding("user1", true, OffsetDateTime.now().plusHours(1));
        when(syncBindingRepository.findAll()).thenReturn(List.of(b));

        scheduler.retryFailedSyncs();

        verifyNoInteractions(googleKeepSyncService);
    }

    @Test
    void retryFailedSyncs_callsSyncWhenRetryAtIsInPast() {
        SyncBinding b = binding("user1", true, OffsetDateTime.now().minusMinutes(1));
        when(syncBindingRepository.findAll()).thenReturn(List.of(b));

        scheduler.retryFailedSyncs();

        verify(googleKeepSyncService).syncNow("user1");
    }

    @Test
    void retryFailedSyncs_swallowsExceptionFromSyncNow() {
        SyncBinding b = binding("user1", true, null);
        when(syncBindingRepository.findAll()).thenReturn(List.of(b));
        doThrow(new RuntimeException("sync failure")).when(googleKeepSyncService).syncNow("user1");

        assertDoesNotThrow(() -> scheduler.retryFailedSyncs());
    }

    // --- helpers ---

    private SyncBinding binding(String userKey, boolean enabled, OffsetDateTime nextRetryAt) {
        SyncBinding b = new SyncBinding();
        b.setUserKey(userKey);
        b.setProvider(GoogleKeepSyncService.PROVIDER);
        b.setRemoteNoteId("note-1");
        b.setEnabled(enabled);
        b.setNextRetryAt(nextRetryAt);
        return b;
    }
}
