package com.holodos.integrations.googlekeep.infrastructure;

import com.holodos.integrations.googlekeep.domain.SyncBinding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncBindingRepository extends JpaRepository<SyncBinding, Long> {
    Optional<SyncBinding> findByUserKeyAndProvider(String userKey, String provider);
}
