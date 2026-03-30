package com.holodos.integrations.googletasks.domain;

import com.holodos.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "google_tasks_bindings")
public class GoogleTasksBinding extends BaseEntity {

    @Column(name = "user_key", nullable = false, unique = true)
    private String userKey;

    @Column(name = "task_list_id")
    private String taskListId;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "last_sync_status", length = 50)
    private String lastSyncStatus;

    @Column(name = "failure_count", nullable = false)
    private int failureCount = 0;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }

    public String getTaskListId() { return taskListId; }
    public void setTaskListId(String taskListId) { this.taskListId = taskListId; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public OffsetDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(OffsetDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }

    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }

    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }

    public OffsetDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(OffsetDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
}
