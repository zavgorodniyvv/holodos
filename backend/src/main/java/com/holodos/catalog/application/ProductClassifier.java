package com.holodos.catalog.application;

import java.util.List;
import java.util.Optional;

/**
 * Guesses catalog defaults for a product known only by name. Implementations must never throw:
 * an empty result means "no guess", and the caller falls back to deterministic defaults.
 */
public interface ProductClassifier {

    Optional<Classification> classify(String productName, List<Option> categories, List<Option> units, List<Option> storagePlaces);

    record Option(Long id, String name) {}

    record Classification(Long categoryId, Long unitId, Long storagePlaceId, Integer shelfLifeDays) {}
}
