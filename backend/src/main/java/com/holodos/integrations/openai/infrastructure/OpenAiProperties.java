package com.holodos.integrations.openai.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "holodos.integrations.openai")
public record OpenAiProperties(
    boolean enabled,
    String baseUrl,
    String apiKey,
    String model,
    /** Sent as reasoning.effort when set; leave blank for models without reasoning controls. */
    String reasoningEffort,
    Duration timeout
) {
    public String baseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
    }

    public Duration timeout() {
        return timeout == null ? Duration.ofSeconds(10) : timeout;
    }
}
