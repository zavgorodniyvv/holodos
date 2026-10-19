package com.holodos.inventory.infrastructure;

import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long>, JpaSpecificationExecutor<StockEntry> {
    List<StockEntry> findByProductIdAndStatus(Long productId, StockStatus status);

    /** Total quantity of a product in the given status, excluding one batch (the one being changed). */
    @Query("select coalesce(sum(s.quantity), 0) from StockEntry s "
        + "where s.product.id = :productId and s.status = :status and s.id <> :excludedId")
    BigDecimal sumQuantityByProductAndStatusExcluding(@Param("productId") Long productId,
                                                      @Param("status") StockStatus status,
                                                      @Param("excludedId") Long excludedId);

    /** Same as above, restricted to batches measured in the given unit. */
    @Query("select coalesce(sum(s.quantity), 0) from StockEntry s "
        + "where s.product.id = :productId and s.status = :status and s.unit.id = :unitId and s.id <> :excludedId")
    BigDecimal sumQuantityByProductStatusAndUnitExcluding(@Param("productId") Long productId,
                                                          @Param("status") StockStatus status,
                                                          @Param("unitId") Long unitId,
                                                          @Param("excludedId") Long excludedId);

    @Query("select s from StockEntry s join fetch s.product p join fetch p.category join fetch s.storagePlace where s.status = :status")
    List<StockEntry> findWithProductByStatus(@Param("status") StockStatus status);

    List<StockEntry> findByStatus(StockStatus status);

}
