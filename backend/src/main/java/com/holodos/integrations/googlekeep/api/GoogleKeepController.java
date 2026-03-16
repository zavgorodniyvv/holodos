package com.holodos.integrations.googlekeep.api;

import com.holodos.integrations.googlekeep.application.GoogleKeepSyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integrations/google-keep")
public class GoogleKeepController {
    private final GoogleKeepSyncService googleKeepSyncService;

    public GoogleKeepController(GoogleKeepSyncService googleKeepSyncService) {
        this.googleKeepSyncService = googleKeepSyncService;
    }

    @PostMapping("/bind")
    public BindResponse bind(@Valid @RequestBody BindRequest request) {
        var binding = googleKeepSyncService.bind(request.userKey(), request.remoteNoteId());
        return new BindResponse(binding.getId(), binding.getUserKey(), binding.getRemoteNoteId(), binding.isEnabled());
    }

    @PostMapping("/sync")
    public SyncResponse sync(@Valid @RequestBody SyncRequest request) {
        String details = googleKeepSyncService.syncNow(request.userKey());
        return new SyncResponse(details);
    }

    @PostMapping("/sync-inbound")
    public SyncResponse syncInbound(@Valid @RequestBody SyncRequest request) {
        return new SyncResponse(googleKeepSyncService.syncInbound(request.userKey()));
    }

    @PostMapping("/retry-failed")
    public SyncResponse retryFailed(@Valid @RequestBody SyncRequest request) {
        return new SyncResponse(googleKeepSyncService.retryLastFailed(request.userKey()));
    }

    public record BindRequest(@NotBlank String userKey, @NotBlank String remoteNoteId) {}
    public record BindResponse(Long id, String userKey, String remoteNoteId, boolean enabled) {}
    public record SyncRequest(@NotBlank String userKey) {}
    public record SyncResponse(String details) {}
}
