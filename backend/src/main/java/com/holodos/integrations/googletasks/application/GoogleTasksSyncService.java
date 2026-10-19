package com.holodos.integrations.googletasks.application;

import com.holodos.common.domain.ConflictException;
import com.holodos.common.domain.ExternalServiceException;
import com.holodos.integrations.googletasks.application.GoogleTasksClient.RemoteTask;
import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksBindingRepository;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class GoogleTasksSyncService {
    private static final Logger log = LoggerFactory.getLogger(GoogleTasksSyncService.class);
    private static final String SHOPPING_PREFIX = "shopping-";
    private static final Duration BASE_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Duration MAX_RETRY_DELAY = Duration.ofHours(2);

    private final GoogleTasksBindingRepository bindingRepository;
    private final GoogleTasksClient googleTasksClient;
    private final GoogleTasksOAuthService oAuthService;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final TransactionOperations transactions;
    /** Scheduler and manual sync must not run concurrently for one user: both would create the same remote tasks. */
    private final Map<String, ReentrantLock> syncLocks = new ConcurrentHashMap<>();

    public GoogleTasksSyncService(GoogleTasksBindingRepository bindingRepository,
                                  GoogleTasksClient googleTasksClient,
                                  GoogleTasksOAuthService oAuthService,
                                  ShoppingListItemRepository shoppingListItemRepository,
                                  TransactionOperations transactions) {
        this.bindingRepository = bindingRepository;
        this.googleTasksClient = googleTasksClient;
        this.oAuthService = oAuthService;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<GoogleTasksBinding> listBindings() {
        return bindingRepository.findAll();
    }

    /**
     * The sync and the failure bookkeeping run in separate transactions, so failure count / retry time
     * survive the rollback of a failed sync.
     */
    public String syncNow(String userKey) {
        ReentrantLock lock = syncLocks.computeIfAbsent(userKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ConflictException("Google Tasks sync is already running for user: " + userKey);
        }
        try {
            transactions.executeWithoutResult(status -> checkSyncAllowed(loadBinding(userKey)));
            try {
                return transactions.execute(status -> doSync(userKey));
            } catch (RuntimeException ex) {
                int failures = transactions.execute(status -> recordFailure(userKey, ex.getMessage()));
                log.error("Google Tasks sync failed for user: {} (attempt {})", userKey, failures, ex);
                throw new ExternalServiceException("Google Tasks sync failed", ex);
            }
        } finally {
            lock.unlock();
        }
    }

    private String doSync(String userKey) {
        GoogleTasksBinding binding = loadBinding(userKey);
        String token = oAuthService.getValidAccessToken(binding);
        if (binding.getTaskListId() == null) {
            String listId = googleTasksClient.createTaskList("Holodos Shopping", token);
            binding.setTaskListId(listId);
            bindingRepository.save(binding);
            log.info("Created Google Tasks list '{}' for user: {}", listId, userKey);
        }
        List<RemoteTask> remoteTasks = googleTasksClient.listTasks(binding.getTaskListId(), token);
        // Index remote tasks by their externalId (stored in notes). Duplicates (e.g. left by an earlier
        // interrupted sync) must not break every later sync: keep the first one.
        Map<String, RemoteTask> remoteByExternalId = remoteTasks.stream()
                .filter(t -> t.notes() != null && t.notes().startsWith(SHOPPING_PREFIX))
                .collect(Collectors.toMap(RemoteTask::notes, t -> t, (first, duplicate) -> first));
        // Apply inbound completions: tasks marked complete in Google Tasks → complete in Holodos
        remoteTasks.stream()
                .filter(RemoteTask::completed)
                .filter(t -> t.notes() != null && t.notes().startsWith(SHOPPING_PREFIX))
                .forEach(remoteTask -> {
                    Long shoppingId = parseShoppingId(remoteTask.notes());
                    if (shoppingId != null) {
                        shoppingListItemRepository.findById(shoppingId).ifPresent(item -> {
                            if (item.getStatus() == ShoppingItemStatus.ACTIVE) {
                                item.setStatus(ShoppingItemStatus.COMPLETED);
                                item.setCompletedAt(OffsetDateTime.now());
                                shoppingListItemRepository.save(item);
                                log.debug("Marked shopping item {} as completed (from Google Tasks)", shoppingId);
                            }
                        });
                    }
                });
        // Reconcile outbound: active shopping items not yet in Google Tasks → create them
        shoppingListItemRepository.findByStatusOrderBySortOrderAsc(ShoppingItemStatus.ACTIVE)
                .forEach(item -> {
                    String externalId = SHOPPING_PREFIX + item.getId();
                    if (!remoteByExternalId.containsKey(externalId)) {
                        googleTasksClient.createTask(binding.getTaskListId(), item.getTitle(), externalId, token);
                        log.debug("Created Google Tasks task for shopping item: {}", item.getId());
                    }
                });
        // Apply outbound completions: items completed in Holodos but still needsAction in Google Tasks
        remoteByExternalId.values().stream()
                .filter(remoteTask -> !remoteTask.completed())
                .forEach(remoteTask -> {
                    Long shoppingId = parseShoppingId(remoteTask.notes());
                    if (shoppingId != null) {
                        shoppingListItemRepository.findById(shoppingId).ifPresent(item -> {
                            if (item.getStatus() == ShoppingItemStatus.COMPLETED) {
                                googleTasksClient.completeTask(binding.getTaskListId(), remoteTask.id(), token);
                                log.debug("Completed Google Tasks task {} for shopping item {}", remoteTask.id(), shoppingId);
                            }
                        });
                    }
                });
        binding.setLastSyncedAt(OffsetDateTime.now());
        binding.setLastSyncStatus("SUCCESS");
        binding.setFailureCount(0);
        binding.setLastErrorMessage(null);
        binding.setNextRetryAt(null);
        bindingRepository.save(binding);
        log.info("Google Tasks sync completed successfully for user: {}", userKey);
        return "Sync completed successfully";
    }

    private GoogleTasksBinding loadBinding(String userKey) {
        return bindingRepository.findByUserKey(userKey)
                .orElseThrow(() -> new IllegalArgumentException("Google Tasks binding not found for user: " + userKey));
    }

    private void checkSyncAllowed(GoogleTasksBinding binding) {
        if (!binding.isEnabled()) {
            throw new IllegalArgumentException("Google Tasks sync is disabled");
        }
        if (binding.getNextRetryAt() != null && binding.getNextRetryAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Sync retry scheduled after " + binding.getNextRetryAt());
        }
    }

    private int recordFailure(String userKey, String message) {
        GoogleTasksBinding binding = loadBinding(userKey);
        int failures = binding.getFailureCount() + 1;
        binding.setFailureCount(failures);
        binding.setLastErrorMessage(message);
        long delayMinutes = Math.min(BASE_RETRY_DELAY.multipliedBy(failures).toMinutes(), MAX_RETRY_DELAY.toMinutes());
        binding.setNextRetryAt(OffsetDateTime.now().plusMinutes(delayMinutes));
        binding.setLastSyncStatus("FAILED");
        bindingRepository.save(binding);
        return failures;
    }

    private Long parseShoppingId(String notes) {
        if (notes == null || !notes.startsWith(SHOPPING_PREFIX)) {
            return null;
        }
        try {
            return Long.parseLong(notes.substring(SHOPPING_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
