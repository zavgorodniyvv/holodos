package com.holodos.settings.api;

import com.holodos.settings.application.UserSettingsService;
import com.holodos.settings.domain.UserSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final UserSettingsService userSettingsService;

    public SettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public SettingsResponse get(@RequestParam(defaultValue = "default") String userKey) {
        return map(userSettingsService.getOrCreate(userKey));
    }

    @PutMapping
    public SettingsResponse upsert(@Valid @RequestBody SettingsRequest request) {
        UserSettings settings = new UserSettings();
        settings.setUserKey(request.userKey());
        settings.setExpiryDaysBeforeNotify(request.expiryDaysBeforeNotify());
        settings.setNotifyExpiring(request.notifyExpiring());
        settings.setNotifyExpired(request.notifyExpired());
        settings.setNotifyOldItems(request.notifyOldItems());
        settings.setNotifyOutOfStock(request.notifyOutOfStock());
        settings.setQuietHoursStart(request.quietHoursStart());
        settings.setQuietHoursEnd(request.quietHoursEnd());
        settings.setMaxFrequencyMinutes(request.maxFrequencyMinutes());
        return map(userSettingsService.upsert(settings));
    }

    private SettingsResponse map(UserSettings s) {
        return new SettingsResponse(
            s.getUserKey(),
            s.getExpiryDaysBeforeNotify(),
            s.isNotifyExpiring(),
            s.isNotifyExpired(),
            s.isNotifyOldItems(),
            s.isNotifyOutOfStock(),
            s.getQuietHoursStart(),
            s.getQuietHoursEnd(),
            s.getMaxFrequencyMinutes()
        );
    }

    public record SettingsRequest(
        @NotBlank String userKey,
        @NotNull @Positive Integer expiryDaysBeforeNotify,
        @NotNull Boolean notifyExpiring,
        @NotNull Boolean notifyExpired,
        @NotNull Boolean notifyOldItems,
        @NotNull Boolean notifyOutOfStock,
        String quietHoursStart,
        String quietHoursEnd,
        @NotNull @Positive Integer maxFrequencyMinutes
    ) {}

    public record SettingsResponse(
        String userKey,
        Integer expiryDaysBeforeNotify,
        Boolean notifyExpiring,
        Boolean notifyExpired,
        Boolean notifyOldItems,
        Boolean notifyOutOfStock,
        String quietHoursStart,
        String quietHoursEnd,
        Integer maxFrequencyMinutes
    ) {}
}
