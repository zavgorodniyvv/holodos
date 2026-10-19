package com.holodos.shopping.infrastructure;

import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long>, JpaSpecificationExecutor<ShoppingListItem> {
    Optional<ShoppingListItem> findFirstByProductIdAndStatus(Long productId, ShoppingItemStatus status);
    @EntityGraph(attributePaths = {"store"})
    List<ShoppingListItem> findByStatusOrderBySortOrderAsc(ShoppingItemStatus status);

    /**
     * Serializes concurrent auto-replenishment for one product until the surrounding transaction ends,
     * so two batches emptied at the same time cannot both insert an entry (PostgreSQL advisory lock).
     */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(:productId)) as lock", nativeQuery = true)
    Integer lockProductForReplenishment(@Param("productId") Long productId);

}
