package com.holodos.integrations.googletasks.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksBindingRepository;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksProperties;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GoogleTasksOAuthServiceTest {
    @Mock GoogleTasksBindingRepository bindingRepository;

    MockRestServiceServer google;
    GoogleTasksOAuthService service;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        google = MockRestServiceServer.bindTo(restTemplate).build();
        service = new GoogleTasksOAuthService(new GoogleTasksProperties("id", "secret", "http://localhost/cb"),
            bindingRepository, restTemplate);
    }

    @Test
    void reauthorizationClearsFailureBackoff() {
        GoogleTasksBinding binding = new GoogleTasksBinding();
        binding.setUserKey("default");
        binding.setRefreshToken("old-refresh");
        binding.setFailureCount(57);
        binding.setNextRetryAt(OffsetDateTime.now().plusHours(2));
        binding.setLastErrorMessage("401 Unauthorized");
        when(bindingRepository.findByUserKey("default")).thenReturn(Optional.of(binding));
        google.expect(requestTo("https://oauth2.googleapis.com/token")).andRespond(withSuccess(
            "{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\",\"expires_in\":3600}",
            MediaType.APPLICATION_JSON));

        service.handleCallback("code", "default");

        assertEquals("new-access", binding.getAccessToken());
        assertEquals("new-refresh", binding.getRefreshToken());
        assertTrue(binding.isEnabled());
        assertEquals(0, binding.getFailureCount());
        assertNull(binding.getNextRetryAt());
        assertNull(binding.getLastErrorMessage());
        verify(bindingRepository).save(binding);
        google.verify();
    }
}
