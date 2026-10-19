package com.holodos.integrations.googlekeep.application;

import com.holodos.common.domain.ConflictException;
import com.holodos.common.domain.ExternalServiceException;
import com.holodos.common.infrastructure.CorrelationIdFilter;
import com.holodos.integrations.googlekeep.domain.SyncBinding;
import com.holodos.integrations.googlekeep.domain.SyncEvent;
import com.holodos.integrations.googlekeep.infrastructure.SyncBindingRepository;
import com.holodos.integrations.googlekeep.infrastructure.SyncEventRepository;
import com.holodos.shopping.domain.ShoppingItemSource;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.domain.ShoppingListItem;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.util.StringUtils;

@Service
public class GoogleKeepSyncService {
    public static final String PROVIDER = "GOOGLE_KEEP";
    private static final Duration BASE_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Duration MAX_RETRY_DELAY = Duration.ofHours(2);

    private final SyncBindingRepository syncBindingRepository;
    private final SyncEventRepository syncEventRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final GoogleKeepClient googleKeepClient;
    private final TransactionOperations transactions;

    public GoogleKeepSyncService(SyncBindingRepository syncBindingRepository, SyncEventRepository syncEventRepository,
                                 ShoppingListItemRepository shoppingListItemRepository, GoogleKeepClient googleKeepClient,
                                 TransactionOperations transactions) {
        this.syncBindingRepository = syncBindingRepository;
        this.syncEventRepository = syncEventRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.googleKeepClient = googleKeepClient;
        this.transactions = transactions;
    }

    @Transactional
    public SyncBinding bind(String userKey, String remoteNoteId) {
        SyncBinding binding = syncBindingRepository.findByUserKeyAndProvider(userKey, PROVIDER).orElseGet(SyncBinding::new);
        binding.setUserKey(userKey);
        binding.setProvider(PROVIDER);
        binding.setRemoteNoteId(remoteNoteId);
        binding.setEnabled(true);
        return syncBindingRepository.save(binding);
    }

    @Transactional(readOnly = true)
    public java.util.List<SyncBinding> listBindings() {
        return syncBindingRepository.findAll();
    }

    /**
     * The sync itself and the failure bookkeeping run in separate transactions: a failed sync rolls back
     * everything it imported, while the failure count / retry time / FAILED event are still committed.
     */
    public String syncNow(String userKey) {
        transactions.executeWithoutResult(status -> checkSyncAllowed(loadBinding(userKey)));
        String idempotencyKey = "sync-" + userKey + "-" + UUID.randomUUID();
        try {
            return transactions.execute(status -> doSync(userKey, idempotencyKey));
        } catch (RuntimeException ex) {
            String details = ex.getMessage();
            transactions.executeWithoutResult(status -> {
                SyncBinding binding = loadBinding(userKey);
                handleFailure(binding, details);
                recordEvent(binding, idempotencyKey, "FAILED", details);
            });
            throw ex instanceof ExternalServiceException ese ? ese : new ExternalServiceException("Google Keep sync failed", ex);
        }
    }

    private String doSync(String userKey, String idempotencyKey) {
        SyncBinding binding = loadBinding(userKey);
        GoogleKeepClient.KeepRemoteState remoteState = googleKeepClient.fetchChecklist(binding.getRemoteNoteId());
        applyRemoteState(remoteState, binding.getLastSyncedAt());
        binding.setLastRemoteEtag(remoteState.etag());
        List<GoogleKeepClient.KeepChecklistItem> outboundItems = buildOutboundItems();
        GoogleKeepClient.KeepSyncResult result = googleKeepClient.pushChecklist(binding.getRemoteNoteId(), outboundItems, binding.getLastRemoteEtag());
        if (!result.success()) {
            // Throwing rolls back the items imported above, so a retry won't import them a second time
            throw new ExternalServiceException(result.details());
        }
        recordEvent(binding, idempotencyKey, "SUCCESS", result.details());
        binding.setLastRemoteEtag(result.newEtag());
        markSyncSuccess(binding);
        return result.details();
    }

    private SyncBinding loadBinding(String userKey) {
        return syncBindingRepository.findByUserKeyAndProvider(userKey, PROVIDER)
            .orElseThrow(() -> new IllegalArgumentException("Google Keep binding not found"));
    }

    private void checkSyncAllowed(SyncBinding binding) {
        if (!binding.isEnabled()) {
            throw new IllegalArgumentException("Google Keep sync is disabled");
        }
        if (binding.getNextRetryAt() != null && binding.getNextRetryAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Sync retry scheduled after " + binding.getNextRetryAt());
        }
    }

    private void applyRemoteState(GoogleKeepClient.KeepRemoteState state, OffsetDateTime lastSyncedAt) {
        if (state == null || state.items() == null) {
            return;
        }
        for (GoogleKeepClient.KeepChecklistItem item : state.items()) {
            Long shoppingId = parseShoppingId(item.externalId());
            if (shoppingId != null) {
                shoppingListItemRepository.findById(shoppingId).ifPresent(existing -> {
                    if (item.checked() && existing.getStatus() == ShoppingItemStatus.ACTIVE) {
                        existing.setStatus(ShoppingItemStatus.COMPLETED);
                        existing.setCompletedAt(OffsetDateTime.now());
                        shoppingListItemRepository.save(existing);
                    } else if (!item.checked() && existing.getStatus() == ShoppingItemStatus.COMPLETED
                        && completedBeforeLastSync(existing, lastSyncedAt)) {
                        // Unchecked in Keep after we last pushed: the remote change is newer, reopen the item.
                        // Items completed locally since the last sync win; the outbound push removes them from Keep.
                        existing.setStatus(ShoppingItemStatus.ACTIVE);
                        existing.setCompletedAt(null);
                        shoppingListItemRepository.save(existing);
                    }
                });
            } else if (!item.checked()) {
                ShoppingListItem newItem = new ShoppingListItem();
                newItem.setTitle(item.text());
                newItem.setQuantity(BigDecimal.ONE);
                newItem.setSource(ShoppingItemSource.KEEP_SYNC);
                newItem.setStatus(ShoppingItemStatus.ACTIVE);
                newItem.setSortOrder(0);
                shoppingListItemRepository.save(newItem);
            }
        }
    }

    private static boolean completedBeforeLastSync(ShoppingListItem item, OffsetDateTime lastSyncedAt) {
        return lastSyncedAt != null && item.getCompletedAt() != null && item.getCompletedAt().isBefore(lastSyncedAt);
    }

    private List<GoogleKeepClient.KeepChecklistItem> buildOutboundItems() {
        return shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE).stream()
            .map(i -> new GoogleKeepClient.KeepChecklistItem(i.getTitle(), false, i.getId() == null ? null : "shopping-" + i.getId()))
            .toList();
    }

    private void recordEvent(SyncBinding binding, String idempotencyKey, String status, String details) {
        SyncEvent event = new SyncEvent();
        event.setBinding(binding);
        event.setEventType("SHOPPING_LIST_SYNC");
        event.setDirection("BIDIRECTIONAL");
        event.setStatus(status);
        event.setDetails(details);
        event.setIdempotencyKey(idempotencyKey);
        event.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        syncEventRepository.save(event);
    }

    private void markSyncSuccess(SyncBinding binding) {
        binding.setLastSyncedAt(OffsetDateTime.now());
        binding.setFailureCount(0);
        binding.setLastErrorMessage(null);
        binding.setNextRetryAt(null);
        binding.setLastSyncStatus("SUCCESS");
        syncBindingRepository.save(binding);
    }

    private void handleFailure(SyncBinding binding, String details) {
        int failures = binding.getFailureCount() + 1;
        binding.setFailureCount(failures);
        binding.setLastErrorMessage(details);
        long delayMinutes = Math.min(BASE_RETRY_DELAY.multipliedBy(failures).toMinutes(), MAX_RETRY_DELAY.toMinutes());
        binding.setNextRetryAt(OffsetDateTime.now().plusMinutes(delayMinutes));
        binding.setLastSyncStatus("FAILED");
        syncBindingRepository.save(binding);
    }

    private Long parseShoppingId(String externalId) {
        if (!StringUtils.hasText(externalId) || !externalId.startsWith("shopping-")) {
            return null;
        }
        try {
            return Long.parseLong(externalId.substring("shopping-".length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
