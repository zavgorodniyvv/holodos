package com.holodos.catalog.infrastructure;

import com.holodos.catalog.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
