package com.holodos.integrations.googlekeep.application;

import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
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
        syncBindingRepository.findAll().stream()
            .filter(b -> b.isEnabled())
            .forEach(b -> googleKeepSyncService.syncNow(b.getUserKey()));
    }
}
