package com.holodos.catalog.infrastructure;

import com.holodos.catalog.domain.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<UnitOfMeasure, Long> {
}
