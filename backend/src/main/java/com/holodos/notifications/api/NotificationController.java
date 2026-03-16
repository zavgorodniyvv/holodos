package com.holodos.notifications.api;

import com.holodos.notifications.application.NotificationService;
import com.holodos.notifications.domain.Notification;
import com.holodos.notifications.domain.NotificationStatus;
import java.time.OffsetDateTime;
import java.util.List;
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
        return notificationService.list().stream().map(this::map).toList();
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(id);
    }

    private NotificationResponse map(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getEntityType(), n.getEntityId(), n.getStatus(), n.getCreatedAt(), n.getReadAt());
    }

    public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        String entityType,
        String entityId,
        NotificationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
    ) {}
}
