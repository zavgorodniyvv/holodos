package com.holodos.catalog.domain.repository;

import com.holodos.catalog.domain.model.UnitOfMeasure;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<UnitOfMeasure, UUID> {
}
