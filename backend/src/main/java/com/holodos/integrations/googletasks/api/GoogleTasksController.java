package com.holodos.integrations.googletasks.api;

import com.holodos.integrations.googletasks.application.GoogleTasksOAuthService;
import com.holodos.integrations.googletasks.application.GoogleTasksSyncService;
import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
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

    private final GoogleTasksSyncService syncService;
    private final GoogleTasksOAuthService oAuthService;

    public GoogleTasksController(GoogleTasksSyncService syncService, GoogleTasksOAuthService oAuthService) {
        this.syncService = syncService;
        this.oAuthService = oAuthService;
    }

    @GetMapping("/auth-url")
    public AuthUrlResponse getAuthUrl(@RequestParam String userKey) {
        return new AuthUrlResponse(oAuthService.buildAuthUrl(userKey));
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<Void> oauthCallback(@RequestParam String code, @RequestParam String state) {
        oAuthService.handleCallback(code, state);
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
