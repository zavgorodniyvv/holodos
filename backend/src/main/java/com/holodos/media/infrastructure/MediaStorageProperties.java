package com.holodos.media.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "holodos.media")
public record MediaStorageProperties(Provider provider, String rootPath, S3 s3) {
    public enum Provider { FILESYSTEM, S3 }

    public record S3(String bucket, String accessKey, String secretKey, String region, String endpoint, boolean pathStyleAccess) {
    }
}
