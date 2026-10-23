package com.holodos.notifications.application;

import com.holodos.inventory.infrastructure.StockEntryRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
    /** Notification settings are household-wide; the web UI edits this key. */
    public static final String DEFAULT_USER_KEY = "default";

    private final NotificationRepository notificationRepository;
    private final UserSettingsService userSettingsService;
    private final StockEntryRepository stockEntryRepository;

    public NotificationService(NotificationRepository notificationRepository, UserSettingsService userSettingsService,
                               StockEntryRepository stockEntryRepository) {
        this.notificationRepository = notificationRepository;
        this.userSettingsService = userSettingsService;
        this.stockEntryRepository = stockEntryRepository;
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

    public void markAllRead() {
        List<Notification> unread = notificationRepository.findByStatusInOrderByCreatedAtDesc(
            List.of(NotificationStatus.NEW, NotificationStatus.DELIVERED));
        OffsetDateTime now = OffsetDateTime.now();
        for (Notification notification : unread) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }

    /**
     * Product of each notification about a stock entry, keyed by notification id. The batch itself may be
     * consumed or discarded by now (out-of-stock notifications), so it is looked up regardless of status.
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> productIds(Collection<Notification> notifications) {
        Map<Long, Long> stockEntryIds = new HashMap<>();
        for (Notification n : notifications) {
            if ("StockEntry".equals(n.getEntityType()) && n.getEntityId() != null) {
                try {
                    stockEntryIds.put(n.getId(), Long.valueOf(n.getEntityId()));
                } catch (NumberFormatException ignored) {
                    // not a stock entry id: no product
                }
            }
        }
        if (stockEntryIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> productByEntry = new HashMap<>();
        for (Object[] row : stockEntryRepository.findProductIdsByIds(new ArrayList<>(stockEntryIds.values()))) {
            productByEntry.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, Long> result = new HashMap<>();
        stockEntryIds.forEach((notificationId, entryId) -> {
            Long productId = productByEntry.get(entryId);
            if (productId != null) {
                result.put(notificationId, productId);
            }
        });
        return result;
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
