package com.holodos.integrations.googlekeep.application;

import com.holodos.common.infrastructure.CorrelationIdFilter;
import com.holodos.integrations.googlekeep.domain.SyncBinding;
import com.holodos.integrations.googlekeep.domain.SyncEvent;
import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
import com.holodos.integrations.googlekeep.infrastructure.SyncEventRepository;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoogleKeepSyncService {
    public static final String PROVIDER = "GOOGLE_KEEP";

    private final SyncBindingRepository syncBindingRepository;
    private final SyncEventRepository syncEventRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final GoogleKeepClient googleKeepClient;

    public GoogleKeepSyncService(SyncBindingRepository syncBindingRepository, SyncEventRepository syncEventRepository,
                                 ShoppingListItemRepository shoppingListItemRepository, GoogleKeepClient googleKeepClient) {
        this.syncBindingRepository = syncBindingRepository;
        this.syncEventRepository = syncEventRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.googleKeepClient = googleKeepClient;
    }

    public SyncBinding bind(String userKey, String remoteNoteId) {
        SyncBinding binding = syncBindingRepository.findByUserKeyAndProvider(userKey, PROVIDER).orElseGet(SyncBinding::new);
        binding.setUserKey(userKey);
        binding.setProvider(PROVIDER);
        binding.setRemoteNoteId(remoteNoteId);
        binding.setEnabled(true);
        return syncBindingRepository.save(binding);
    }

    public String syncNow(String userKey) {
        SyncBinding binding = syncBindingRepository.findByUserKeyAndProvider(userKey, PROVIDER)
            .orElseThrow(() -> new IllegalArgumentException("Google Keep binding not found"));
        if (!binding.isEnabled()) {
            throw new IllegalArgumentException("Google Keep sync is disabled");
        }

        String idempotencyKey = "sync-" + userKey + "-" + UUID.randomUUID();
        var items = shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE).stream()
            .map(i -> new GoogleKeepClient.KeepChecklistItem(i.getTitle(), false, "shopping-" + i.getId()))
            .toList();

        GoogleKeepClient.KeepSyncResult result = googleKeepClient.pushChecklist(binding.getRemoteNoteId(), items, binding.getLastRemoteEtag());

        SyncEvent event = new SyncEvent();
        event.setBinding(binding);
        event.setEventType("SHOPPING_LIST_SYNC");
        event.setDirection("OUTBOUND");
        event.setStatus(result.success() ? "SUCCESS" : "FAILED");
        event.setDetails(result.details());
        event.setIdempotencyKey(idempotencyKey);
        event.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        syncEventRepository.save(event);

        if (result.success()) {
            binding.setLastRemoteEtag(result.newEtag());
            binding.setLastSyncedAt(OffsetDateTime.now());
            syncBindingRepository.save(binding);
        }

        return result.details();
    }
}
