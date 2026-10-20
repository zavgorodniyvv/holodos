package com.holodos.integrations.openai.infrastructure;

import com.holodos.catalog.application.ProductClassifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiIntegrationConfig {

    /** The RestTemplate is not a bean on purpose: GoogleTasksOAuthService injects the only RestTemplate bean by type. */
    @Bean
    @ConditionalOnProperty(prefix = "holodos.integrations.openai", name = "enabled", havingValue = "true")
    public ProductClassifier openAiProductClassifier(OpenAiProperties properties, JsonMapper jsonMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.timeout());
        return new OpenAiProductClassifier(new RestTemplate(factory), properties, jsonMapper);
    }

    @Bean
    @ConditionalOnMissingBean(ProductClassifier.class)
    public ProductClassifier noopProductClassifier() {
        return new NoopProductClassifier();
    }
}
