package com.holodos.integrations.googlekeep.api;

import com.holodos.integrations.googlekeep.application.GoogleKeepSyncService;
import com.holodos.integrations.googlekeep.domain.SyncBinding;
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
        return map(binding);
    }

    @GetMapping("/bindings")
    public java.util.List<BindResponse> bindings() {
        return googleKeepSyncService.listBindings().stream().map(this::map).toList();
    }

    @PostMapping("/sync")
    public SyncResponse sync(@Valid @RequestBody SyncRequest request) {
        String details = googleKeepSyncService.syncNow(request.userKey());
        return new SyncResponse(details);
    }

    private BindResponse map(SyncBinding binding) {
        return new BindResponse(binding.getId(), binding.getUserKey(), binding.getRemoteNoteId(), binding.isEnabled(),
            binding.getLastSyncedAt(), binding.getFailureCount(), binding.getLastErrorMessage(), binding.getNextRetryAt(), binding.getLastSyncStatus());
    }

    public record BindRequest(@NotBlank String userKey, @NotBlank String remoteNoteId) {}
    public record BindResponse(Long id, String userKey, String remoteNoteId, boolean enabled,
                               java.time.OffsetDateTime lastSyncedAt, int failureCount,
                               String lastErrorMessage, java.time.OffsetDateTime nextRetryAt, String lastSyncStatus) {}
    public record SyncRequest(@NotBlank String userKey) {}
    public record SyncResponse(String details) {}
}
