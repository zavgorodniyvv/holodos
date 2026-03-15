package com.holodos.purchases.infrastructure;

import com.holodos.purchases.domain.PurchaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseEventRepository extends JpaRepository<PurchaseEvent, Long> {
}
