package com.holodos.catalog.infrastructure;

import com.holodos.catalog.domain.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    /** Listings render category/unit/place/store names, so load them in the same query. */
    @Override
    @EntityGraph(attributePaths = {"category", "defaultUnit", "defaultStoragePlace", "defaultStore"})
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "defaultUnit", "defaultStoragePlace", "defaultStore"})
    Page<Product> findByNameContainingIgnoreCase(String search, Pageable pageable);

    Optional<Product> findFirstByNameIgnoreCase(String name);
}
