package com.holodos.inventory.infrastructure;

import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long>, JpaSpecificationExecutor<StockEntry> {
    List<StockEntry> findByProductIdAndStatus(Long productId, StockStatus status);
}
