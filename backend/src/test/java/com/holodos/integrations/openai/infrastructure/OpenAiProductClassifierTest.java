package com.holodos.integrations.openai.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.holodos.catalog.application.ProductClassifier.Classification;
import com.holodos.catalog.application.ProductClassifier.Option;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

class OpenAiProductClassifierTest {
    RestTemplate restTemplate = mock(RestTemplate.class);
    OpenAiProductClassifier classifier = new OpenAiProductClassifier(restTemplate,
        new OpenAiProperties(true, null, "sk-test", "gpt-test", "none", Duration.ofSeconds(5)), JsonMapper.builder().build());

    List<Option> categories = List.of(new Option(1L, "Food"), new Option(2L, "Household"));
    List<Option> units = List.of(new Option(1L, "Piece (pcs)"), new Option(4L, "Liter (l)"));
    List<Option> places = List.of(new Option(1L, "Fridge"), new Option(3L, "Pantry"));

    @Test
    void parsesStructuredOutput() {
        String response = """
            {"status":"completed","output":[
              {"type":"reasoning","summary":[]},
              {"type":"message","role":"assistant","content":[
                {"type":"output_text","text":"{\\"categoryId\\":1,\\"unitId\\":4,\\"storagePlaceId\\":1,\\"shelfLifeDays\\":7}"}
              ]}
            ]}""";
        when(restTemplate.postForObject(eq("https://api.openai.com/v1/responses"), any(), eq(String.class))).thenReturn(response);

        Optional<Classification> result = classifier.classify("Молоко", categories, units, places);

        assertEquals(Optional.of(new Classification(1L, 4L, 1L, 7)), result);
    }

    @Test
    void refusalAndHttpErrorsGiveEmptyResult() {
        when(restTemplate.postForObject(any(String.class), any(), eq(String.class)))
            .thenReturn("{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\",\"refusal\":\"no\"}]}]}")
            .thenThrow(new ResourceAccessException("timeout"));

        assertFalse(classifier.classify("Молоко", categories, units, places).isPresent());
        assertFalse(classifier.classify("Молоко", categories, units, places).isPresent());
    }

    @Test
    @SuppressWarnings("unchecked")
    void requestRestrictsIdsToDictionaries() {
        Map<String, Object> body = classifier.requestBody("Молоко", categories, units, places);

        assertEquals("gpt-test", body.get("model"));
        assertEquals(Map.of("effort", "none"), body.get("reasoning"));
        Map<String, Object> format = (Map<String, Object>) ((Map<String, Object>) body.get("text")).get("format");
        assertEquals(true, format.get("strict"));
        Map<String, Object> props = (Map<String, Object>) ((Map<String, Object>) format.get("schema")).get("properties");
        assertEquals(List.of(1L, 3L), ((Map<String, Object>) props.get("storagePlaceId")).get("enum"));
        assertTrue(((String) body.get("input")).contains("4: Liter (l)"));
    }
}
