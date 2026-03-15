package com.holodos.settings.infrastructure;

import com.holodos.settings.domain.UserSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUserKey(String userKey);
}
