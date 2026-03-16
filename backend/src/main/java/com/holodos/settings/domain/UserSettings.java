package com.holodos.settings.domain;

import com.holodos.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings extends BaseEntity {
    @Column(name = "user_key", nullable = false, unique = true)
    private String userKey;

    @Column(name = "expiry_days_before_notify", nullable = false)
    private Integer expiryDaysBeforeNotify = 3;

    @Column(name = "notify_expiring", nullable = false)
    private boolean notifyExpiring = true;

    @Column(name = "notify_expired", nullable = false)
    private boolean notifyExpired = true;

    @Column(name = "notify_old_items", nullable = false)
    private boolean notifyOldItems = true;

    @Column(name = "notify_out_of_stock", nullable = false)
    private boolean notifyOutOfStock = true;

    @Column(name = "quiet_hours_start")
    private String quietHoursStart;

    @Column(name = "quiet_hours_end")
    private String quietHoursEnd;

    @Column(name = "max_frequency_minutes", nullable = false)
    private Integer maxFrequencyMinutes = 360;

    public String getUserKey() { return userKey; }
    public void setUserKey(String userKey) { this.userKey = userKey; }
    public Integer getExpiryDaysBeforeNotify() { return expiryDaysBeforeNotify; }
    public void setExpiryDaysBeforeNotify(Integer expiryDaysBeforeNotify) { this.expiryDaysBeforeNotify = expiryDaysBeforeNotify; }
    public boolean isNotifyExpiring() { return notifyExpiring; }
    public void setNotifyExpiring(boolean notifyExpiring) { this.notifyExpiring = notifyExpiring; }
    public boolean isNotifyExpired() { return notifyExpired; }
    public void setNotifyExpired(boolean notifyExpired) { this.notifyExpired = notifyExpired; }
    public boolean isNotifyOldItems() { return notifyOldItems; }
    public void setNotifyOldItems(boolean notifyOldItems) { this.notifyOldItems = notifyOldItems; }
    public boolean isNotifyOutOfStock() { return notifyOutOfStock; }
    public void setNotifyOutOfStock(boolean notifyOutOfStock) { this.notifyOutOfStock = notifyOutOfStock; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
    public Integer getMaxFrequencyMinutes() { return maxFrequencyMinutes; }
    public void setMaxFrequencyMinutes(Integer maxFrequencyMinutes) { this.maxFrequencyMinutes = maxFrequencyMinutes; }
}
