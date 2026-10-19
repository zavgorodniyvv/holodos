package com.holodos.notifications.application;

import com.holodos.notifications.domain.Notification;
import com.holodos.notifications.domain.NotificationStatus;
import com.holodos.notifications.infrastructure.NotificationRepository;
import com.holodos.settings.application.UserSettingsService;
import com.holodos.settings.domain.UserSettings;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
    /** Notification settings are household-wide; the web UI edits this key. */
    public static final String DEFAULT_USER_KEY = "default";

    private final NotificationRepository notificationRepository;
    private final UserSettingsService userSettingsService;

    public NotificationService(NotificationRepository notificationRepository, UserSettingsService userSettingsService) {
        this.notificationRepository = notificationRepository;
        this.userSettingsService = userSettingsService;
    }

    public Notification createIfMissing(String dedupKey, String type, String title, String message, String entityType, String entityId) {
        return notificationRepository.findByDedupKey(dedupKey).orElseGet(() -> {
            Notification n = new Notification();
            n.setDedupKey(dedupKey);
            n.setType(type);
            n.setTitle(title);
            n.setMessage(message);
            n.setEntityType(entityType);
            n.setEntityId(entityId);
            n.setStatus(NotificationStatus.NEW);
            return notificationRepository.save(n);
        });
    }

    /** Called when the last available batch of a product is consumed, discarded or adjusted to zero. */
    public void notifyOutOfStock(String productName, Long stockEntryId) {
        if (!settings().isNotifyOutOfStock()) {
            return;
        }
        createIfMissing("outofstock:" + stockEntryId, "OUT_OF_STOCK", "Продукт закончился",
            productName + " закончился", "StockEntry", String.valueOf(stockEntryId));
    }

    /** Newest first: every unread notification plus the 100 most recent read ones (the table only grows). */
    @Transactional(readOnly = true)
    public List<Notification> list() {
        List<Notification> result = new ArrayList<>(
            notificationRepository.findByStatusInOrderByCreatedAtDesc(List.of(NotificationStatus.NEW, NotificationStatus.DELIVERED)));
        result.addAll(notificationRepository.findTop100ByStatusOrderByCreatedAtDesc(NotificationStatus.READ));
        result.sort(Comparator.comparing(Notification::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public void markRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    public UserSettings settings() {
        return userSettingsService.getOrCreate(DEFAULT_USER_KEY);
    }

    public int expiryLeadDays(String userKey) {
        return userSettingsService.getOrCreate(userKey).getExpiryDaysBeforeNotify();
    }

    /** True when a notification of one of the given types was created less than maxFrequencyMinutes ago. */
    @Transactional(readOnly = true)
    public boolean isThrottled(UserSettings settings, Collection<String> types, OffsetDateTime now) {
        Integer minutes = settings.getMaxFrequencyMinutes();
        if (minutes == null || minutes <= 0) {
            return false;
        }
        return notificationRepository.findFirstByTypeInOrderByCreatedAtDesc(types)
            .map(Notification::getCreatedAt)
            .filter(last -> last != null && last.isAfter(now.minusMinutes(minutes)))
            .isPresent();
    }

    /** Quiet hours are "HH:mm" strings; a window like 22:00–07:00 wraps around midnight. Invalid values are ignored. */
    public static boolean isQuietTime(UserSettings settings, LocalTime time) {
        if (settings.getQuietHoursStart() == null || settings.getQuietHoursEnd() == null) {
            return false;
        }
        try {
            LocalTime start = LocalTime.parse(settings.getQuietHoursStart());
            LocalTime end = LocalTime.parse(settings.getQuietHoursEnd());
            if (start.equals(end)) {
                return false;
            }
            return start.isBefore(end)
                ? !time.isBefore(start) && time.isBefore(end)
                : !time.isBefore(start) || time.isBefore(end);
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
