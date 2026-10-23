package com.holodos.notifications.api;

import com.holodos.notifications.application.NotificationService;
import com.holodos.notifications.domain.Notification;
import com.holodos.notifications.domain.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        List<Notification> notifications = notificationService.list();
        Map<Long, Long> productIds = notificationService.productIds(notifications);
        return notifications.stream().map(n -> map(n, productIds.get(n.getId()))).toList();
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(id);
    }

    private NotificationResponse map(Notification n, Long productId) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getEntityType(), n.getEntityId(),
            productId, n.getStatus(), n.getCreatedAt(), n.getReadAt());
    }

    public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String entityType,
        String entityId,
        /** Product of the stock entry the notification is about, when there is one. */
        Long productId,
        NotificationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
    ) {}
}
