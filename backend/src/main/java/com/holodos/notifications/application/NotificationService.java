package com.holodos.notifications.application;

import com.holodos.common.domain.NotFoundException;
import com.holodos.notifications.domain.Notification;
import com.holodos.notifications.domain.NotificationStatus;
import com.holodos.notifications.infrastructure.NotificationRepository;
import com.holodos.settings.application.UserSettingsService;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
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

    @Transactional(readOnly = true)
    public List<Notification> list() {
        return notificationRepository.findAll();
    }

    public void markRead(Long id) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new NotFoundException("Notification not found"));
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(OffsetDateTime.now());
        notificationRepository.save(notification);
    }

    public int expiryLeadDays(String userKey) {
        return userSettingsService.getOrCreate(userKey).getExpiryDaysBeforeNotify();
    }
}
