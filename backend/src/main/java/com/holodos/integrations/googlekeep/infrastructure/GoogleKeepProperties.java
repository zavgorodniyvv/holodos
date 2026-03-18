package com.holodos.integrations.googlekeep.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "holodos.integrations.google-keep")
public record GoogleKeepProperties(
    boolean enabled,
    String baseUrl,
    String apiKey,
    Duration timeout
) {
    public Duration timeout() {
        return timeout == null ? Duration.ofSeconds(10) : timeout;
    }
}
