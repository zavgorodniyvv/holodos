package com.holodos.inventory.infrastructure;

import com.holodos.inventory.domain.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
}
