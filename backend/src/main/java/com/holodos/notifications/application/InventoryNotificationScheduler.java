package com.holodos.notifications.application;

import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.settings.domain.UserSettings;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryNotificationScheduler {
    static final List<String> SCAN_TYPES = List.of("EXPIRED", "EXPIRING_SOON", "STORED_TOO_LONG");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final StockEntryRepository stockEntryRepository;
    private final NotificationService notificationService;
    private final ZoneId zone;

    public InventoryNotificationScheduler(StockEntryRepository stockEntryRepository, NotificationService notificationService,
                                          @Value("${holodos.time-zone:Europe/Kyiv}") String zone) {
        this.stockEntryRepository = stockEntryRepository;
        this.notificationService = notificationService;
        this.zone = ZoneId.of(zone);
    }

    @Transactional
    @Scheduled(cron = "0 */30 * * * *")
    public void scanExpiryAndOldItems() {
        scan(OffsetDateTime.now());
    }

    void scan(OffsetDateTime now) {
        UserSettings settings = notificationService.settings();
        if (NotificationService.isQuietTime(settings, now.atZoneSameInstant(zone).toLocalTime())
            || notificationService.isThrottled(settings, SCAN_TYPES, now)) {
            return;
        }
        // Expiry is a calendar date stored as midnight UTC (that's what the UI sends); "today" is the household's date
        LocalDate today = now.atZoneSameInstant(zone).toLocalDate();
        LocalDate expiringBoundary = today.plusDays(settings.getExpiryDaysBeforeNotify());

        for (StockEntry s : stockEntryRepository.findWithProductByStatus(StockStatus.AVAILABLE)) {
            String id = String.valueOf(s.getId());
            String name = s.getProduct().getName();
            if (s.getExpiresAt() != null) {
                LocalDate expiry = utcDate(s.getExpiresAt());
                if (expiry.isBefore(today)) {
                    if (settings.isNotifyExpired()) {
                        notificationService.createIfMissing("expired:" + id, "EXPIRED", "Срок годности истёк",
                            name + ": срок годности истёк " + DATE.format(expiry), "StockEntry", id);
                    }
                } else if (!expiry.isAfter(expiringBoundary) && settings.isNotifyExpiring()) {
                    notificationService.createIfMissing("expiring:" + id, "EXPIRING_SOON", "Скоро истекает срок",
                        name + ": годен до " + DATE.format(expiry), "StockEntry", id);
                }
            }
            if (settings.isNotifyOldItems() && s.getAddedAt() != null && s.getAddedAt().isBefore(now.minusDays(365))) {
                notificationService.createIfMissing("olditem:" + id, "STORED_TOO_LONG", "Хранится больше года",
                    name + ": хранится с " + DATE.format(utcDate(s.getAddedAt())), "StockEntry", id);
            }
        }
    }

    private static LocalDate utcDate(OffsetDateTime value) {
        return value.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }
}
