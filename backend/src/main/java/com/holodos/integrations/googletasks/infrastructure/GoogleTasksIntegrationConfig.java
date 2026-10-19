package com.holodos.integrations.googletasks.infrastructure;

import com.holodos.integrations.googletasks.application.GoogleTasksClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(GoogleTasksProperties.class)
public class GoogleTasksIntegrationConfig {

    /** Explicit timeouts: these calls run on the shared scheduler thread, a hung request would stall all jobs. */
    @Bean
    public RestTemplate googleTasksRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    @Bean
    public GoogleTasksClient googleTasksClient() {
        return new HttpGoogleTasksClient(googleTasksRestTemplate());
    }
}
