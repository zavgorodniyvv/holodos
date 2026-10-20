package com.holodos.integrations.openai.infrastructure;

import com.holodos.catalog.application.ProductClassifier;
import java.util.List;
import java.util.Optional;

/** Used when the OpenAI integration is disabled: callers fall back to default category/unit/place. */
public class NoopProductClassifier implements ProductClassifier {

    @Override
    public Optional<Classification> classify(String productName, List<Option> categories, List<Option> units, List<Option> storagePlaces) {
        return Optional.empty();
    }
}
