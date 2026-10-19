package com.holodos.integrations.googletasks.api;

import com.holodos.integrations.googletasks.application.GoogleTasksOAuthService;
import com.holodos.integrations.googletasks.application.GoogleTasksSyncService;
import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/google-tasks")
public class GoogleTasksController {
    private static final String OAUTH_STATES_ATTRIBUTE = GoogleTasksController.class.getName() + ".oauthStates";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GoogleTasksSyncService syncService;
    private final GoogleTasksOAuthService oAuthService;

    public GoogleTasksController(GoogleTasksSyncService syncService, GoogleTasksOAuthService oAuthService) {
        this.syncService = syncService;
        this.oAuthService = oAuthService;
    }

    @GetMapping("/auth-url")
    public AuthUrlResponse getAuthUrl(@RequestParam String userKey, HttpSession session) {
        // OAuth "state" is a random nonce bound to this session (CSRF protection): a callback carrying
        // someone else's code cannot be attached to our user key
        String state = generateState();
        pendingStates(session).put(state, userKey);
        return new AuthUrlResponse(oAuthService.buildAuthUrl(state));
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<Void> oauthCallback(@RequestParam String code, @RequestParam String state, HttpSession session) {
        String userKey = pendingStates(session).remove(state);
        if (userKey == null) {
            throw new IllegalArgumentException("Unknown or expired OAuth state, start the authorization again");
        }
        oAuthService.handleCallback(code, userKey);
        return ResponseEntity.status(302)
                .location(URI.create("/admin/index.html?google-tasks=connected"))
                .build();
    }

    @PostMapping("/sync")
    public SyncResponse sync(@Valid @RequestBody SyncRequest request) {
        String details = syncService.syncNow(request.userKey());
        return new SyncResponse(details);
    }

    @GetMapping("/bindings")
    public List<BindingResponse> bindings() {
        return syncService.listBindings().stream()
                .map(this::mapBinding)
                .toList();
    }

    private BindingResponse mapBinding(GoogleTasksBinding binding) {
        return new BindingResponse(
                binding.getId(),
                binding.getUserKey(),
                binding.getTaskListId(),
                binding.isEnabled(),
                binding.getLastSyncedAt(),
                binding.getLastSyncStatus(),
                binding.getFailureCount(),
                binding.getLastErrorMessage()
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> pendingStates(HttpSession session) {
        synchronized (session) {
            Object states = session.getAttribute(OAUTH_STATES_ATTRIBUTE);
            if (states == null) {
                states = new ConcurrentHashMap<String, String>();
                session.setAttribute(OAUTH_STATES_ATTRIBUTE, states);
            }
            return (Map<String, String>) states;
        }
    }

    private static String generateState() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record AuthUrlResponse(String authUrl) {}

    public record SyncRequest(@NotBlank String userKey) {}

    public record SyncResponse(String details) {}

    public record BindingResponse(
            Long id,
            String userKey,
            String taskListId,
            boolean enabled,
            OffsetDateTime lastSyncedAt,
            String lastSyncStatus,
            int failureCount,
            String lastErrorMessage
    ) {}
}
