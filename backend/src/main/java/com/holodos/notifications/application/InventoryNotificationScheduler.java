package com.holodos.notifications.application;

import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import java.time.OffsetDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InventoryNotificationScheduler {
    private final StockEntryRepository stockEntryRepository;
    private final NotificationService notificationService;

    public InventoryNotificationScheduler(StockEntryRepository stockEntryRepository, NotificationService notificationService) {
        this.stockEntryRepository = stockEntryRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void scanExpiryAndOldItems() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime expiringBoundary = now.plusDays(notificationService.expiryLeadDays("default"));
        stockEntryRepository.findAll().stream()
            .filter(s -> s.getStatus() == StockStatus.AVAILABLE)
            .forEach(s -> {
                if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(expiringBoundary)) {
                    notificationService.createIfMissing(
                        "expiring:" + s.getId(),
                        "EXPIRING_SOON",
                        "Item expiring soon",
                        s.getProduct().getName() + " expires at " + s.getExpiresAt(),
                        "StockEntry",
                        String.valueOf(s.getId())
                    );
                }
                if (s.getAddedAt() != null && s.getAddedAt().isBefore(now.minusDays(365))) {
                    notificationService.createIfMissing(
                        "olditem:" + s.getId(),
                        "STORED_TOO_LONG",
                        "Item stored too long",
                        s.getProduct().getName() + " has been stored for over one year",
                        "StockEntry",
                        String.valueOf(s.getId())
                    );
                }
            });
    }
}
