package com.holodos.purchases.infrastructure;

import com.holodos.purchases.domain.PurchaseEvent;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseEventRepository extends JpaRepository<PurchaseEvent, Long> {
    long countByPurchasedAtBetween(OffsetDateTime from, OffsetDateTime to);

    long countByPurchasedAtBetweenAndStoreId(OffsetDateTime from, OffsetDateTime to, Long storeId);
}
