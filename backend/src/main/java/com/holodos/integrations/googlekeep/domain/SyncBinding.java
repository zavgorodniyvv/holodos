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
}
