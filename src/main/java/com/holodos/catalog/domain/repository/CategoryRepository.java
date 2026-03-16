package com.holodos.catalog.domain.repository;

import com.holodos.catalog.domain.model.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
