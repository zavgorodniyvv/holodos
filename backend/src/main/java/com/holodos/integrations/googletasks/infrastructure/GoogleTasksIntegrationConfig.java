package com.holodos.integrations.googletasks.infrastructure;

import com.holodos.integrations.googletasks.application.GoogleTasksClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(GoogleTasksProperties.class)
public class GoogleTasksIntegrationConfig {

    /**
     * Explicit timeouts: these calls run on the shared scheduler thread, a hung request would stall all jobs.
     * JDK HttpClient rather than HttpURLConnection, which rejects PATCH (used to complete tasks).
     */
    @Bean
    public RestTemplate googleTasksRestTemplate() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new RestTemplate(factory);
    }

    @Bean
    public GoogleTasksClient googleTasksClient() {
        return new HttpGoogleTasksClient(googleTasksRestTemplate());
    }
}
