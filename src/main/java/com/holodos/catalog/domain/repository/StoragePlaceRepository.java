package com.holodos.catalog.domain.repository;

import com.holodos.catalog.domain.model.StoragePlace;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoragePlaceRepository extends JpaRepository<StoragePlace, UUID> {
}
