package com.holodos.integrations.openai.infrastructure;

import com.holodos.catalog.application.ProductClassifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Classifies a product by name via the OpenAI Responses API with a strict JSON schema. */
public class OpenAiProductClassifier implements ProductClassifier {
    private static final Logger log = LoggerFactory.getLogger(OpenAiProductClassifier.class);

    private static final String INSTRUCTIONS = """
        You help fill a home inventory catalog. Given a product name (usually in Russian) choose,
        using only ids from the provided lists:
        - the best matching category;
        - the unit it is usually counted in at home (e.g. milk - liters, eggs - pieces, flour - kilograms);
        - where it is usually stored after purchase.
        Also estimate shelfLifeDays: typical shelf life in days of an unopened product stored there,
        or null when it practically does not spoil (salt, detergent, dishes).
        """;

    private final RestTemplate restTemplate;
    private final OpenAiProperties properties;
    private final JsonMapper jsonMapper;

    public OpenAiProductClassifier(RestTemplate restTemplate, OpenAiProperties properties, JsonMapper jsonMapper) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalArgumentException("holodos.integrations.openai.api-key must be set when integration is enabled");
        }
        if (!StringUtils.hasText(properties.model())) {
            throw new IllegalArgumentException("holodos.integrations.openai.model must be set when integration is enabled");
        }
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Optional<Classification> classify(String productName, List<Option> categories, List<Option> units, List<Option> storagePlaces) {
        if (categories.isEmpty() || units.isEmpty() || storagePlaces.isEmpty()) {
            return Optional.empty();
        }
        try {
            String response = restTemplate.postForObject(
                properties.baseUrl() + "/responses",
                new HttpEntity<>(requestBody(productName, categories, units, storagePlaces), headers()),
                String.class
            );
            return parse(response);
        } catch (RuntimeException e) {
            // Never block adding stock because of the AI: the caller falls back to defaults
            log.warn("OpenAI product classification failed for '{}': {}", productName, e.getMessage());
            return Optional.empty();
        }
    }

    Map<String, Object> requestBody(String productName, List<Option> categories, List<Option> units, List<Option> storagePlaces) {
        String input = "Product: " + productName
            + "\n\nCategories:\n" + lines(categories)
            + "\n\nUnits:\n" + lines(units)
            + "\n\nStorage places:\n" + lines(storagePlaces);

        Map<String, Object> schemaProperties = new LinkedHashMap<>();
        schemaProperties.put("categoryId", Map.of("type", "integer", "enum", ids(categories)));
        schemaProperties.put("unitId", Map.of("type", "integer", "enum", ids(units)));
        schemaProperties.put("storagePlaceId", Map.of("type", "integer", "enum", ids(storagePlaces)));
        schemaProperties.put("shelfLifeDays", Map.of("type", List.of("integer", "null")));
        Map<String, Object> schema = Map.of(
            "type", "object",
            "properties", schemaProperties,
            "required", List.copyOf(schemaProperties.keySet()),
            "additionalProperties", false
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("instructions", INSTRUCTIONS);
        body.put("input", input);
        body.put("store", false);
        body.put("max_output_tokens", 1000);
        body.put("text", Map.of("format", Map.of(
            "type", "json_schema",
            "name", "product_classification",
            "strict", true,
            "schema", schema
        )));
        if (StringUtils.hasText(properties.reasoningEffort())) {
            body.put("reasoning", Map.of("effort", properties.reasoningEffort()));
        }
        return body;
    }

    Optional<Classification> parse(String response) {
        if (response == null) {
            return Optional.empty();
        }
        JsonNode root = jsonMapper.readTree(response);
        for (JsonNode item : root.path("output")) {
            if (!"message".equals(item.path("type").asString())) {
                continue;
            }
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asString())) {
                    return Optional.of(jsonMapper.readValue(content.path("text").asString(), Classification.class));
                }
                if ("refusal".equals(content.path("type").asString())) {
                    log.warn("OpenAI refused to classify product: {}", content.path("refusal").asString());
                    return Optional.empty();
                }
            }
        }
        log.warn("OpenAI response has no output text (status {})", root.path("status").asString());
        return Optional.empty();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.apiKey());
        return headers;
    }

    private static String lines(List<Option> options) {
        return options.stream().map(o -> o.id() + ": " + o.name()).collect(Collectors.joining("\n"));
    }

    private static List<Long> ids(List<Option> options) {
        return options.stream().map(Option::id).toList();
    }
}
