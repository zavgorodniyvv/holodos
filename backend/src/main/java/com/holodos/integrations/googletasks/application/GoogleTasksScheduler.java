package com.holodos.integrations.googletasks.application;

import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksBindingRepository;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GoogleTasksScheduler {
    private static final Logger log = LoggerFactory.getLogger(GoogleTasksScheduler.class);

    private final GoogleTasksBindingRepository bindingRepository;
    private final GoogleTasksSyncService syncService;

    public GoogleTasksScheduler(GoogleTasksBindingRepository bindingRepository, GoogleTasksSyncService syncService) {
        this.bindingRepository = bindingRepository;
        this.syncService = syncService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void syncAll() {
        OffsetDateTime now = OffsetDateTime.now();
        bindingRepository.findAll().stream()
            .filter(GoogleTasksBinding::isEnabled)
            .filter(b -> b.getRefreshToken() != null)
            .filter(b -> b.getNextRetryAt() == null || !b.getNextRetryAt().isAfter(now))
            .forEach(b -> {
                try {
                    syncService.syncNow(b.getUserKey());
                } catch (Exception e) {
                    log.warn("Google Tasks sync failed for user {}", b.getUserKey(), e);
                }
            });
    }
}
