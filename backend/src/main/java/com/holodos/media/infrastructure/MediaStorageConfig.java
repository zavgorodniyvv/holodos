package com.holodos.media.infrastructure;

import com.holodos.media.application.MediaStorageGateway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MediaStorageProperties.class)
public class MediaStorageConfig {

    @Bean
    public MediaStorageGateway mediaStorageGateway(MediaStorageProperties properties) {
        if (properties.getProvider() == MediaStorageProperties.Provider.S3) {
            return new S3MediaStorageGateway(properties);
        }
        return new FilesystemMediaStorageGateway(properties);
    }
}
