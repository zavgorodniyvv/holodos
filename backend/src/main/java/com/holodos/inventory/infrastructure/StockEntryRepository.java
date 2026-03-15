package com.holodos.inventory.infrastructure;

import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {
    List<StockEntry> findByProductIdAndStatus(Long productId, StockStatus status);
}
