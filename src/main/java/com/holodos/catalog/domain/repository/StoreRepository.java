package com.holodos.catalog.domain.repository;

import com.holodos.catalog.domain.model.Store;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, UUID> {
}
