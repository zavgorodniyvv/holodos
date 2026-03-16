package com.holodos.settings.application;

import com.holodos.settings.domain.UserSettings;
import com.holodos.settings.infrastructure.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserSettingsService {
    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsService(UserSettingsRepository userSettingsRepository) {
        this.userSettingsRepository = userSettingsRepository;
    }

    public UserSettings getOrCreate(String userKey) {
        return userSettingsRepository.findByUserKey(userKey).orElseGet(() -> {
            UserSettings settings = new UserSettings();
            settings.setUserKey(userKey);
            return userSettingsRepository.save(settings);
        });
    }

    public UserSettings upsert(UserSettings updated) {
        UserSettings settings = getOrCreate(updated.getUserKey());
        settings.setExpiryDaysBeforeNotify(updated.getExpiryDaysBeforeNotify());
        settings.setNotifyExpiring(updated.isNotifyExpiring());
        settings.setNotifyExpired(updated.isNotifyExpired());
        settings.setNotifyOldItems(updated.isNotifyOldItems());
        settings.setNotifyOutOfStock(updated.isNotifyOutOfStock());
        settings.setQuietHoursStart(updated.getQuietHoursStart());
        settings.setQuietHoursEnd(updated.getQuietHoursEnd());
        settings.setMaxFrequencyMinutes(updated.getMaxFrequencyMinutes());
        return userSettingsRepository.save(settings);
    }
}
