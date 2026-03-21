package com.holodos.common.infrastructure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api/docs/**", "/actuator/**").permitAll()
                .anyRequest().permitAll());
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(Customizer.withDefaults());
        }
        return http.build();
    }
}
