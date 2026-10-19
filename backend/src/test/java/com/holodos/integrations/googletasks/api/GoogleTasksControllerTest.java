package com.holodos.integrations.googletasks.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.holodos.common.api.GlobalExceptionHandler;
import com.holodos.integrations.googletasks.application.GoogleTasksOAuthService;
import com.holodos.integrations.googletasks.application.GoogleTasksSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GoogleTasksControllerTest {
    GoogleTasksSyncService syncService = mock(GoogleTasksSyncService.class);
    GoogleTasksOAuthService oAuthService = mock(GoogleTasksOAuthService.class);
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new GoogleTasksController(syncService, oAuthService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void callbackWithStateIssuedToThisSessionLinksTheRequestedUser() throws Exception {
        when(oAuthService.buildAuthUrl(anyString())).thenReturn("https://accounts.google.com/auth");
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/api/integrations/google-tasks/auth-url").param("userKey", "slava").session(session))
            .andExpect(status().isOk());
        ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
        verify(oAuthService).buildAuthUrl(state.capture());

        mockMvc.perform(get("/api/integrations/google-tasks/oauth2/callback")
                .param("code", "c1").param("state", state.getValue()).session(session))
            .andExpect(status().isFound());
        verify(oAuthService).handleCallback("c1", "slava");

        // A state can be used only once
        mockMvc.perform(get("/api/integrations/google-tasks/oauth2/callback")
                .param("code", "c2").param("state", state.getValue()).session(session))
            .andExpect(status().isBadRequest());
    }

    @Test
    void callbackWithForeignStateIsRejected() throws Exception {
        // The old implementation used the user key itself as state, so this used to bind the attacker's code
        mockMvc.perform(get("/api/integrations/google-tasks/oauth2/callback")
                .param("code", "attacker-code").param("state", "slava").session(new MockHttpSession()))
            .andExpect(status().isBadRequest());
        verify(oAuthService, never()).handleCallback(anyString(), anyString());
    }
}
