package com.holodos.notifications.infrastructure;

import com.holodos.notifications.domain.Notification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByDedupKey(String dedupKey);
}
