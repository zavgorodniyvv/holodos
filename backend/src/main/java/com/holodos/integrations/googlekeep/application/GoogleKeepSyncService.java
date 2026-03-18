package com.holodos.integrations.googlekeep.application;

import com.holodos.common.domain.NotFoundException;
import com.holodos.common.infrastructure.CorrelationIdFilter;
import com.holodos.integrations.googlekeep.domain.SyncBinding;
import com.holodos.integrations.googlekeep.domain.SyncEvent;
import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
import com.holodos.integrations.googlekeep.infrastructure.SyncEventRepository;
import com.holodos.purchases.api.PurchaseDtos.ProcessPurchaseRequest;
import com.holodos.purchases.application.PurchaseService;
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
    private final PurchaseService purchaseService;

    public GoogleKeepSyncService(SyncBindingRepository syncBindingRepository, SyncEventRepository syncEventRepository,
                                 ShoppingListItemRepository shoppingListItemRepository, GoogleKeepClient googleKeepClient,
                                 PurchaseService purchaseService) {
        this.syncBindingRepository = syncBindingRepository;
        this.syncEventRepository = syncEventRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.googleKeepClient = googleKeepClient;
        this.purchaseService = purchaseService;
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
        SyncBinding binding = getEnabledBinding(userKey);

        String idempotencyKey = "sync-out-" + userKey + "-" + UUID.randomUUID();
        var items = shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE).stream()
            .map(i -> new GoogleKeepClient.KeepChecklistItem(i.getTitle(), false, "shopping-" + i.getId()))
            .toList();

        GoogleKeepClient.KeepSyncResult result = googleKeepClient.pushChecklist(binding.getRemoteNoteId(), items, binding.getLastRemoteEtag());
        saveEvent(binding, "SHOPPING_LIST_SYNC", "OUTBOUND", result.success() ? "SUCCESS" : "FAILED", result.details(), idempotencyKey);

        if (result.success()) {
            binding.setLastRemoteEtag(result.newEtag());
            binding.setLastSyncedAt(OffsetDateTime.now());
            syncBindingRepository.save(binding);
        }

        return result.details();
    }

    public String syncInbound(String userKey) {
        SyncBinding binding = getEnabledBinding(userKey);
        GoogleKeepClient.KeepRemoteState remote = googleKeepClient.fetchChecklist(binding.getRemoteNoteId());

        int processed = 0;
        for (GoogleKeepClient.KeepChecklistItem item : remote.items()) {
            if (!item.checked()) {
                continue;
            }
            String idem = "sync-in-" + (remote.etag() == null ? "noetag" : remote.etag()) + "-" + item.externalId();
            if (syncEventRepository.findFirstByIdempotencyKey(idem).isPresent()) {
                continue;
            }

            Long shoppingItemId = extractShoppingItemId(item.externalId());
            if (shoppingItemId == null) {
                saveEvent(binding, "KEEP_ITEM_CHECKED", "INBOUND", "SKIPPED", "Unsupported externalId: " + item.externalId(), idem);
                continue;
            }

            var shoppingItem = shoppingListItemRepository.findById(shoppingItemId).orElse(null);
            if (shoppingItem == null || shoppingItem.getStatus() != ShoppingItemStatus.ACTIVE) {
                saveEvent(binding, "KEEP_ITEM_CHECKED", "INBOUND", "SKIPPED", "Shopping item not active: " + shoppingItemId, idem);
                continue;
            }

            try {
                purchaseService.processPurchase(new ProcessPurchaseRequest(
                    shoppingItemId,
                    shoppingItem.getQuantity(),
                    shoppingItem.getProduct() == null ? null : shoppingItem.getProduct().getDefaultStoragePlace().getId(),
                    OffsetDateTime.now(),
                    null,
                    "Processed from Google Keep"
                ));
                processed++;
                saveEvent(binding, "KEEP_ITEM_CHECKED", "INBOUND", "SUCCESS", "Processed shopping item " + shoppingItemId, idem);
            } catch (Exception e) {
                saveEvent(binding, "KEEP_ITEM_CHECKED", "INBOUND", "FAILED", e.getMessage(), idem);
            }
        }

        binding.setLastRemoteEtag(remote.etag());
        binding.setLastSyncedAt(OffsetDateTime.now());
        syncBindingRepository.save(binding);
        return "Processed checked items: " + processed;
    }

    public String retryLastFailed(String userKey) {
        SyncBinding binding = getEnabledBinding(userKey);
        boolean hasFailed = syncEventRepository.findTopByBindingIdAndStatusOrderByIdDesc(binding.getId(), "FAILED").isPresent();
        if (!hasFailed) {
            return "No failed sync events";
        }
        String out = syncNow(userKey);
        String inbound = syncInbound(userKey);
        return "Retry completed. Outbound: " + out + "; Inbound: " + inbound;
    }

    private SyncBinding getEnabledBinding(String userKey) {
        SyncBinding binding = syncBindingRepository.findByUserKeyAndProvider(userKey, PROVIDER)
            .orElseThrow(() -> new NotFoundException("Google Keep binding not found"));
        if (!binding.isEnabled()) {
            throw new IllegalArgumentException("Google Keep sync is disabled");
        }
        return binding;
    }

    private void saveEvent(SyncBinding binding, String eventType, String direction, String status, String details, String idemKey) {
        SyncEvent event = new SyncEvent();
        event.setBinding(binding);
        event.setEventType(eventType);
        event.setDirection(direction);
        event.setStatus(status);
        event.setDetails(details);
        event.setIdempotencyKey(idemKey);
        event.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        syncEventRepository.save(event);
    }

    private Long extractShoppingItemId(String externalId) {
        if (externalId == null || !externalId.startsWith("shopping-")) {
            return null;
        }
        try {
            return Long.parseLong(externalId.substring("shopping-".length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
