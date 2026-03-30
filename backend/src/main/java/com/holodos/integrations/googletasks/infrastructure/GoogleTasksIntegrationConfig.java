package com.holodos.integrations.googletasks.infrastructure;

import com.holodos.integrations.googletasks.application.GoogleTasksClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(GoogleTasksProperties.class)
public class GoogleTasksIntegrationConfig {

    @Bean
    public RestTemplate googleTasksRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public GoogleTasksClient googleTasksClient() {
        return new HttpGoogleTasksClient(googleTasksRestTemplate());
    }
}
