package com.holodos.integrations.googlekeep.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sync_events")
public class SyncEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "binding_id", nullable = false)
    private SyncBinding binding;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String direction;

    @Column(nullable = false)
    private String status;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column
    private String details;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public SyncBinding getBinding() { return binding; }
    public String getEventType() { return eventType; }
    public String getDirection() { return direction; }
    public String getStatus() { return status; }
    public String getCorrelationId() { return correlationId; }
    public String getDetails() { return details; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setBinding(SyncBinding binding) { this.binding = binding; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setDirection(String direction) { this.direction = direction; }
    public void setStatus(String status) { this.status = status; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setDetails(String details) { this.details = details; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}
