package com.holodos.notifications.domain;

import com.holodos.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "dedup_key")
    private String dedupKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.NEW;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public OffsetDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(OffsetDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public OffsetDateTime getReadAt() { return readAt; }
    public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
}
