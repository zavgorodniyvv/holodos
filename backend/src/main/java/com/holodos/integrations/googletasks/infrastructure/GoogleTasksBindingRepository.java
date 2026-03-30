package com.holodos.integrations.googletasks.infrastructure;

import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleTasksBindingRepository extends JpaRepository<GoogleTasksBinding, Long> {

    Optional<GoogleTasksBinding> findByUserKey(String userKey);
}
