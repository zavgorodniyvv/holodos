package com.holodos.notifications.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.notifications.domain.Notification;
import com.holodos.notifications.domain.NotificationStatus;
import com.holodos.notifications.infrastructure.NotificationRepository;
import com.holodos.settings.application.UserSettingsService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notificationRepository;
    @Mock UserSettingsService userSettingsService;
    @Mock StockEntryRepository stockEntryRepository;

    NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, userSettingsService, stockEntryRepository);
    }

    @Test
    void createIfMissingIsDeduplicated() {
        Notification existing = new Notification();
        when(notificationRepository.findByDedupKey("k1")).thenReturn(Optional.of(existing));

        Notification n = service.createIfMissing("k1", "EXPIRING_SOON", "t", "m", "StockEntry", "1");

        assertEquals(existing, n);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllReadMarksEveryUnreadNotification() {
        Notification fresh = new Notification();
        fresh.setStatus(NotificationStatus.NEW);
        Notification delivered = new Notification();
        delivered.setStatus(NotificationStatus.DELIVERED);
        when(notificationRepository.findByStatusInOrderByCreatedAtDesc(List.of(NotificationStatus.NEW, NotificationStatus.DELIVERED)))
            .thenReturn(List.of(fresh, delivered));

        service.markAllRead();

        for (Notification n : List.of(fresh, delivered)) {
            assertEquals(NotificationStatus.READ, n.getStatus());
            assertNotNull(n.getReadAt());
        }
        verify(notificationRepository).saveAll(List.of(fresh, delivered));
    }

    @Test
    void productIdsResolvesStockEntryNotificationsOnly() {
        Notification outOfStock = notification(1L, "StockEntry", "10");
        Notification gone = notification(2L, "StockEntry", "11");
        Notification other = notification(3L, "ShoppingListItem", "10");
        when(stockEntryRepository.findProductIdsByIds(any())).thenReturn(List.<Object[]>of(new Object[] {10L, 100L}));

        Map<Long, Long> result = service.productIds(List.of(outOfStock, gone, other));

        assertEquals(Map.of(1L, 100L), result);
    }

    private static Notification notification(Long id, String entityType, String entityId) {
        Notification n = new Notification();
        ReflectionTestUtils.setField(n, "id", id);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        return n;
    }
}
