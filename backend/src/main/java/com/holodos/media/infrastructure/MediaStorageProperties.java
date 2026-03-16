package com.holodos.media.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "holodos.media")
public record MediaStorageProperties(String rootPath) {
}
