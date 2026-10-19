package com.holodos.notifications.infrastructure;

import com.holodos.notifications.domain.Notification;
import com.holodos.notifications.domain.NotificationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByDedupKey(String dedupKey);

    List<Notification> findTop100ByStatusOrderByCreatedAtDesc(NotificationStatus status);

    List<Notification> findByStatusInOrderByCreatedAtDesc(Collection<NotificationStatus> statuses);

    Optional<Notification> findFirstByTypeInOrderByCreatedAtDesc(Collection<String> types);
}
