package com.holodos.integrations.googlekeep.infrastructure;

import com.holodos.integrations.googlekeep.domain.SyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncEventRepository extends JpaRepository<SyncEvent, Long> {
}
