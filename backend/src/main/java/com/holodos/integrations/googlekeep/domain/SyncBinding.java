package com.holodos.integrations.googlekeep.domain;

import com.holodos.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sync_bindings")
public class SyncBinding extends BaseEntity {
    @Column(name = "user_key", nullable = false)
    private String userKey;

    @Column(nullable = false)
    private String provider;

    @Column(name = "remote_note_id", nullable = false)
    private String remoteNoteId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "last_remote_etag")
    private String lastRemoteEtag;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "last_sync_status")
    private String lastSyncStatus;

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getRemoteNoteId() { return remoteNoteId; }
    public void setRemoteNoteId(String remoteNoteId) { this.remoteNoteId = remoteNoteId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public OffsetDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public String getLastRemoteEtag() { return lastRemoteEtag; }
    public void setLastRemoteEtag(String lastRemoteEtag) { this.lastRemoteEtag = lastRemoteEtag; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    public OffsetDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(OffsetDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
}
