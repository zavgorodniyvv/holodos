package com.holodos.integrations.googletasks.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "holodos.integrations.google-tasks")
public record GoogleTasksProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {}
