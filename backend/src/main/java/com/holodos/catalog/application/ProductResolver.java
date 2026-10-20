package com.holodos.catalog.application;

import com.holodos.catalog.api.CatalogDtos.ProductSuggestionResponse;
import com.holodos.catalog.application.ProductClassifier.Classification;
import com.holodos.catalog.application.ProductClassifier.Option;
import com.holodos.catalog.domain.Category;
import com.holodos.catalog.domain.Product;
import com.holodos.catalog.domain.StoragePlace;
import com.holodos.catalog.domain.UnitOfMeasure;
import com.holodos.catalog.infrastructure.CategoryRepository;
import com.holodos.catalog.infrastructure.ProductRepository;
import com.holodos.catalog.infrastructure.StoragePlaceRepository;
import com.holodos.catalog.infrastructure.UnitRepository;
import com.holodos.common.application.DomainEventPublisher;
import com.holodos.common.application.events.OperationLogEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets the user add stock or buy a shopping item by product name alone: finds the product
 * or creates it, with category/unit/place guessed by {@link ProductClassifier} (OpenAI when enabled).
 */
@Service
@Transactional
public class ProductResolver {
    private static final int MAX_SHELF_LIFE_DAYS = 3650;
    private static final String DEFAULT_UNIT_CODE = "piece";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final ProductClassifier productClassifier;
    private final DomainEventPublisher domainEventPublisher;

    public ProductResolver(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        UnitRepository unitRepository,
        StoragePlaceRepository storagePlaceRepository,
        ProductClassifier productClassifier,
        DomainEventPublisher domainEventPublisher
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.storagePlaceRepository = storagePlaceRepository;
        this.productClassifier = productClassifier;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional(readOnly = true)
    public ProductSuggestionResponse suggest(String rawName) {
        String name = normalize(rawName);
        Optional<Product> existing = productRepository.findFirstByNameIgnoreCase(name);
        if (existing.isPresent()) {
            Product p = existing.get();
            return new ProductSuggestionResponse(p.getId(), p.getName(), p.getCategory().getId(), p.getDefaultUnit().getId(),
                p.getDefaultStoragePlace().getId(), p.getShelfLifeDays(), false);
        }
        Guess guess = guess(name);
        return new ProductSuggestionResponse(null, name, guess.category().getId(), guess.unit().getId(),
            guess.storagePlace().getId(), guess.shelfLifeDays(), guess.aiUsed());
    }

    /**
     * Returns the product with this name (case-insensitive), creating it if missing. Values passed by the
     * caller win; only the missing category/unit/place trigger a classifier call.
     */
    public Product resolveOrCreate(String rawName, Long categoryId, Long unitId, Long storagePlaceId, Integer shelfLifeDays) {
        String name = normalize(rawName);
        Optional<Product> existing = productRepository.findFirstByNameIgnoreCase(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        Category category = categoryId == null ? null : categoryRepository.findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        UnitOfMeasure unit = unitId == null ? null : unitRepository.findById(unitId)
            .orElseThrow(() -> new IllegalArgumentException("Unit not found"));
        StoragePlace storagePlace = storagePlaceId == null ? null : storagePlaceRepository.findById(storagePlaceId)
            .orElseThrow(() -> new IllegalArgumentException("Storage place not found"));
        Integer shelfLife = validShelfLife(shelfLifeDays);
        if (category == null || unit == null || storagePlace == null) {
            Guess guess = guess(name);
            if (category == null) category = guess.category();
            if (unit == null) unit = guess.unit();
            if (storagePlace == null) storagePlace = guess.storagePlace();
            if (shelfLife == null) shelfLife = guess.shelfLifeDays();
        }

        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setDefaultUnit(unit);
        product.setDefaultStoragePlace(storagePlace);
        product.setShelfLifeDays(shelfLife);
        // Without a minimum threshold auto-replenishment is meaningless; the user opts in via the admin panel
        product.setAutoAddShopping(false);
        product.setActive(true);
        Product saved = productRepository.save(product);

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", saved.getName());
        payload.put("categoryId", category.getId());
        payload.put("unitId", unit.getId());
        payload.put("storagePlaceId", storagePlace.getId());
        payload.put("shelfLifeDays", shelfLife);
        domainEventPublisher.publish(new OperationLogEvent("PRODUCT_AUTO_CREATE", "Product", String.valueOf(saved.getId()), payload));
        return saved;
    }

    private Guess guess(String name) {
        List<Category> categories = categoryRepository.findAll().stream()
            .filter(Category::isActive)
            .sorted(Comparator.comparing(Category::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Category::getId))
            .toList();
        List<UnitOfMeasure> units = unitRepository.findAll().stream()
            .filter(UnitOfMeasure::isActive)
            .sorted(Comparator.comparing(UnitOfMeasure::getId))
            .toList();
        List<StoragePlace> places = storagePlaceRepository.findAll().stream()
            .filter(StoragePlace::isActive)
            .sorted(Comparator.comparing(StoragePlace::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StoragePlace::getId))
            .toList();
        if (categories.isEmpty() || units.isEmpty() || places.isEmpty()) {
            throw new IllegalArgumentException("Catalog needs at least one active category, unit and storage place");
        }

        Optional<Classification> ai = productClassifier.classify(
            name,
            categories.stream().map(c -> new Option(c.getId(), c.getName())).toList(),
            units.stream().map(u -> new Option(u.getId(), u.getName() + " (" + u.getShortName() + ")")).toList(),
            places.stream().map(p -> new Option(p.getId(), p.getName())).toList()
        );

        // Ids from the model are re-checked: a stale or hallucinated id falls back to the default
        Category category = ai.map(Classification::categoryId)
            .flatMap(id -> categories.stream().filter(c -> c.getId().equals(id)).findFirst())
            .orElse(categories.get(0));
        UnitOfMeasure unit = ai.map(Classification::unitId)
            .flatMap(id -> units.stream().filter(u -> u.getId().equals(id)).findFirst())
            .orElseGet(() -> units.stream().filter(u -> DEFAULT_UNIT_CODE.equals(u.getCode())).findFirst().orElse(units.get(0)));
        StoragePlace place = ai.map(Classification::storagePlaceId)
            .flatMap(id -> places.stream().filter(p -> p.getId().equals(id)).findFirst())
            .orElse(places.get(0));
        Integer shelfLife = ai.map(Classification::shelfLifeDays).map(ProductResolver::validShelfLife).orElse(null);
        return new Guess(category, unit, place, shelfLife, ai.isPresent());
    }

    static String normalize(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("Product name is too long");
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static Integer validShelfLife(Integer days) {
        return days == null || days <= 0 || days > MAX_SHELF_LIFE_DAYS ? null : days;
    }

    private record Guess(Category category, UnitOfMeasure unit, StoragePlace storagePlace, Integer shelfLifeDays, boolean aiUsed) {}
}
