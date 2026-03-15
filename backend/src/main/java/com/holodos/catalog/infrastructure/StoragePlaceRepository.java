package com.holodos.catalog.infrastructure;

import com.holodos.catalog.domain.StoragePlace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoragePlaceRepository extends JpaRepository<StoragePlace, Long> {
}
