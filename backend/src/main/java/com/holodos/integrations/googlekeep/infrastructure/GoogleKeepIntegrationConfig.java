package com.holodos.integrations.googlekeep.infrastructure;

import com.holodos.integrations.googlekeep.application.GoogleKeepClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@EnableConfigurationProperties(GoogleKeepProperties.class)
public class GoogleKeepIntegrationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "holodos.integrations.google-keep", name = "enabled", havingValue = "true")
    public GoogleKeepClient httpGoogleKeepClient(GoogleKeepProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = (int) properties.timeout().toMillis();
        factory.setConnectTimeout(timeoutMillis);
        factory.setReadTimeout(timeoutMillis);
        RestTemplate restTemplate = new RestTemplate(factory);
        return new HttpGoogleKeepClient(restTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean(GoogleKeepClient.class)
    public GoogleKeepClient stubGoogleKeepClient() {
        return new StubGoogleKeepClient();
    }
}
