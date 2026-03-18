package com.holodos.integrations.googlekeep.application;

import com.holodos.integrations.googlekeep.domain.SyncBinding;
import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GoogleKeepRetryScheduler {
    private final SyncBindingRepository syncBindingRepository;
    private final GoogleKeepSyncService googleKeepSyncService;

    public GoogleKeepRetryScheduler(SyncBindingRepository syncBindingRepository, GoogleKeepSyncService googleKeepSyncService) {
        this.syncBindingRepository = syncBindingRepository;
        this.googleKeepSyncService = googleKeepSyncService;
    }

    @Scheduled(cron = "0 */20 * * * *")
    public void retryFailedSyncs() {
        OffsetDateTime now = OffsetDateTime.now();
        syncBindingRepository.findAll().stream()
            .filter(SyncBinding::isEnabled)
            .filter(b -> b.getNextRetryAt() == null || !b.getNextRetryAt().isAfter(now))
            .forEach(b -> {
                try {
                    googleKeepSyncService.syncNow(b.getUserKey());
                } catch (Exception ignored) {
                }
            });
    }
}
