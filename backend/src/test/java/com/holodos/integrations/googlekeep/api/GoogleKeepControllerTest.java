package com.holodos.integrations.googlekeep.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.holodos.common.api.GlobalExceptionHandler;
import com.holodos.integrations.googlekeep.application.GoogleKeepSyncService;
import com.holodos.integrations.googlekeep.domain.SyncBinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class GoogleKeepControllerTest {

    @Mock GoogleKeepSyncService service;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new GoogleKeepController(service))
            .setValidator(validator)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    void bind_returns200WithBindingData() throws Exception {
        SyncBinding binding = new SyncBinding();
        binding.setUserKey("user1");
        binding.setRemoteNoteId("note-abc");
        binding.setEnabled(true);

        when(service.bind("user1", "note-abc")).thenReturn(binding);

        mockMvc.perform(post("/api/integrations/google-keep/bind")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userKey\":\"user1\",\"remoteNoteId\":\"note-abc\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userKey").value("user1"))
            .andExpect(jsonPath("$.remoteNoteId").value("note-abc"))
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void bind_returns400WhenUserKeyIsBlank() throws Exception {
        mockMvc.perform(post("/api/integrations/google-keep/bind")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userKey\":\"\",\"remoteNoteId\":\"note-abc\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void bind_returns400WhenRemoteNoteIdIsBlank() throws Exception {
        mockMvc.perform(post("/api/integrations/google-keep/bind")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userKey\":\"user1\",\"remoteNoteId\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void bindings_returnsListOfAllBindings() throws Exception {
        SyncBinding b1 = new SyncBinding();
        b1.setUserKey("user1");
        b1.setRemoteNoteId("note-1");
        b1.setEnabled(true);

        SyncBinding b2 = new SyncBinding();
        b2.setUserKey("user2");
        b2.setRemoteNoteId("note-2");
        b2.setEnabled(true);

        when(service.listBindings()).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/integrations/google-keep/bindings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void sync_returns200WithDetails() throws Exception {
        when(service.syncNow("user1")).thenReturn("sync completed");

        mockMvc.perform(post("/api/integrations/google-keep/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userKey\":\"user1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.details").value("sync completed"));
    }

    @Test
    void sync_returns400WhenUserKeyIsBlank() throws Exception {
        mockMvc.perform(post("/api/integrations/google-keep/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userKey\":\"\"}"))
            .andExpect(status().isBadRequest());
    }
}
