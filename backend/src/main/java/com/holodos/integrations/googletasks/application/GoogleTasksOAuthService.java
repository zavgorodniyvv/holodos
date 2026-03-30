package com.holodos.integrations.googletasks.application;

import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksBindingRepository;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksProperties;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Transactional
public class GoogleTasksOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTasksOAuthService.class);
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TASKS_SCOPE = "https://www.googleapis.com/auth/tasks";

    private final GoogleTasksProperties properties;
    private final GoogleTasksBindingRepository bindingRepository;
    private final RestTemplate restTemplate;

    public GoogleTasksOAuthService(GoogleTasksProperties properties,
                                   GoogleTasksBindingRepository bindingRepository,
                                   RestTemplate restTemplate) {
        this.properties = properties;
        this.bindingRepository = bindingRepository;
        this.restTemplate = restTemplate;
    }

    public String buildAuthUrl(String userKey) {
        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", TASKS_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", userKey)
                .toUriString();
    }

    public void handleCallback(String code, String userKey) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", properties.clientId());
        params.add("client_secret", properties.clientSecret());
        params.add("redirect_uri", properties.redirectUri());

        TokenResponse tokenResponse = exchangeForTokens(params);

        GoogleTasksBinding binding = bindingRepository.findByUserKey(userKey)
                .orElseGet(GoogleTasksBinding::new);
        binding.setUserKey(userKey);
        binding.setAccessToken(tokenResponse.access_token());
        binding.setRefreshToken(tokenResponse.refresh_token());
        binding.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(tokenResponse.expires_in()));
        binding.setEnabled(true);
        bindingRepository.save(binding);

        log.info("Google Tasks OAuth callback processed for user: {}", userKey);
    }

    public String getValidAccessToken(GoogleTasksBinding binding) {
        if (binding.getTokenExpiresAt() == null
                || OffsetDateTime.now().plusSeconds(60).isAfter(binding.getTokenExpiresAt())) {
            log.debug("Access token expired or about to expire for user: {}; refreshing", binding.getUserKey());
            refreshAccessToken(binding);
        }
        return binding.getAccessToken();
    }

    private void refreshAccessToken(GoogleTasksBinding binding) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", binding.getRefreshToken());
        params.add("client_id", properties.clientId());
        params.add("client_secret", properties.clientSecret());

        TokenResponse tokenResponse = exchangeForTokens(params);

        binding.setAccessToken(tokenResponse.access_token());
        binding.setTokenExpiresAt(OffsetDateTime.now().plusSeconds(tokenResponse.expires_in()));
        bindingRepository.save(binding);

        log.debug("Access token refreshed for user: {}", binding.getUserKey());
    }

    private TokenResponse exchangeForTokens(MultiValueMap<String, String> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        TokenResponse response = restTemplate.postForObject(TOKEN_ENDPOINT, request, TokenResponse.class);
        if (response == null || response.access_token() == null) {
            throw new IllegalStateException("Invalid token response from Google OAuth2 endpoint");
        }
        return response;
    }

    private record TokenResponse(String access_token, String refresh_token, long expires_in) {}
}
