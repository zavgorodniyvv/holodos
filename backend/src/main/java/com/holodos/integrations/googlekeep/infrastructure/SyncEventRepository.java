package com.holodos.integrations.googlekeep.infrastructure;

import com.holodos.integrations.googlekeep.domain.SyncEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncEventRepository extends JpaRepository<SyncEvent, Long> {
    Optional<SyncEvent> findFirstByIdempotencyKey(String idempotencyKey);
    Optional<SyncEvent> findTopByBindingIdAndStatusOrderByIdDesc(Long bindingId, String status);
}
