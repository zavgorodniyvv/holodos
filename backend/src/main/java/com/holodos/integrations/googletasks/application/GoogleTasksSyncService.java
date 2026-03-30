package com.holodos.integrations.googletasks.application;

import com.holodos.integrations.googletasks.application.GoogleTasksClient.RemoteTask;
import com.holodos.integrations.googletasks.domain.GoogleTasksBinding;
import com.holodos.integrations.googletasks.infrastructure.GoogleTasksBindingRepository;
import com.holodos.shopping.domain.ShoppingItemStatus;
import com.holodos.shopping.infrastructure.ShoppingListItemRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GoogleTasksSyncService {

    private static final Logger log = LoggerFactory.getLogger(GoogleTasksSyncService.class);
    private static final String SHOPPING_PREFIX = "shopping-";
    private static final Duration BASE_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Duration MAX_RETRY_DELAY = Duration.ofHours(2);

    private final GoogleTasksBindingRepository bindingRepository;
    private final GoogleTasksClient googleTasksClient;
    private final GoogleTasksOAuthService oAuthService;
    private final ShoppingListItemRepository shoppingListItemRepository;

    public GoogleTasksSyncService(GoogleTasksBindingRepository bindingRepository,
                                  GoogleTasksClient googleTasksClient,
                                  GoogleTasksOAuthService oAuthService,
                                  ShoppingListItemRepository shoppingListItemRepository) {
        this.bindingRepository = bindingRepository;
        this.googleTasksClient = googleTasksClient;
        this.oAuthService = oAuthService;
        this.shoppingListItemRepository = shoppingListItemRepository;
    }

    @Transactional(readOnly = true)
    public List<GoogleTasksBinding> listBindings() {
        return bindingRepository.findAll();
    }

    public String syncNow(String userKey) {
        GoogleTasksBinding binding = bindingRepository.findByUserKey(userKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Google Tasks binding not found for user: " + userKey));

        if (!binding.isEnabled()) {
            throw new IllegalArgumentException("Google Tasks sync is disabled");
        }

        if (binding.getNextRetryAt() != null && binding.getNextRetryAt().isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("Sync retry scheduled after " + binding.getNextRetryAt());
        }

        try {
            String token = oAuthService.getValidAccessToken(binding);

            if (binding.getTaskListId() == null) {
                String listId = googleTasksClient.createTaskList("Holodos Shopping", token);
                binding.setTaskListId(listId);
                bindingRepository.save(binding);
                log.info("Created Google Tasks list '{}' for user: {}", listId, userKey);
            }

            List<RemoteTask> remoteTasks = googleTasksClient.listTasks(binding.getTaskListId(), token);

            // Index remote tasks by their externalId (stored in notes), only those tracking shopping items
            Map<String, RemoteTask> remoteByExternalId = remoteTasks.stream()
                    .filter(t -> t.notes() != null && t.notes().startsWith(SHOPPING_PREFIX))
                    .collect(Collectors.toMap(RemoteTask::notes, t -> t));

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

        } catch (Exception ex) {
            int failures = binding.getFailureCount() + 1;
            binding.setFailureCount(failures);
            binding.setLastErrorMessage(ex.getMessage());
            long delayMinutes = Math.min(BASE_RETRY_DELAY.multipliedBy(failures).toMinutes(),
                    MAX_RETRY_DELAY.toMinutes());
            binding.setNextRetryAt(OffsetDateTime.now().plusMinutes(delayMinutes));
            binding.setLastSyncStatus("FAILED");
            bindingRepository.save(binding);

            log.error("Google Tasks sync failed for user: {} (attempt {})", userKey, failures, ex);
            throw new IllegalStateException("Google Tasks sync failed", ex);
        }
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
