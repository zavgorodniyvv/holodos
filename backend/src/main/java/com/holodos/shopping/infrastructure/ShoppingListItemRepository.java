package com.holodos.shopping.infrastructure;

import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long>, JpaSpecificationExecutor<ShoppingListItem> {
    Optional<ShoppingListItem> findFirstByProductIdAndStatus(Long productId, ShoppingItemStatus status);
    List<ShoppingListItem> findByStatusOrderBySortOrderAsc(ShoppingItemStatus status);
}
