package com.holodos.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "operation_log")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getPayload() { return payload; }
    public String getCorrelationId() { return correlationId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public void setPayload(String payload) { this.payload = payload; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
