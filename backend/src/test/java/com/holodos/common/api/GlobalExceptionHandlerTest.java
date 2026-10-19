package com.holodos.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.holodos.common.domain.ConflictException;
import com.holodos.common.domain.ExternalServiceException;
import com.holodos.common.domain.NotFoundException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void mapsNotFoundExceptionTo404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void malformedJsonIs400NotServerError() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void conflictIs409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void externalServiceFailureIs502() throws Exception {
        mockMvc.perform(get("/test/external"))
            .andExpect(status().isBadGateway());
    }

    @Test
    void optimisticLockIs409() throws Exception {
        mockMvc.perform(get("/test/optimistic"))
            .andExpect(status().isConflict());
    }

    @RestController
    static class TestController {

        @PostMapping("/test/body")
        String body(@RequestBody Map<String, Object> body) {
            return "ok";
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new ConflictException("already done");
        }

        @GetMapping("/test/external")
        String external() {
            throw new ExternalServiceException("google down");
        }

        @GetMapping("/test/optimistic")
        String optimistic() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/test/not-found")
        String throwNotFound() {
            throw new NotFoundException("not found");
        }
    }
}
