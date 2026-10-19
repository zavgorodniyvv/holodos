package com.holodos.notifications.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

import com.holodos.catalog.domain.Product;
import com.holodos.inventory.domain.StockEntry;
import com.holodos.inventory.domain.StockStatus;
import com.holodos.inventory.infrastructure.StockEntryRepository;
import com.holodos.settings.domain.UserSettings;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InventoryNotificationSchedulerTest {
    @Mock StockEntryRepository stockEntryRepository;
    @Mock NotificationService notificationService;

    InventoryNotificationScheduler scheduler;
    UserSettings settings;
    final OffsetDateTime now = OffsetDateTime.of(2026, 10, 19, 12, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        scheduler = new InventoryNotificationScheduler(stockEntryRepository, notificationService, "UTC");
        settings = new UserSettings();
        lenient().when(notificationService.settings()).thenReturn(settings);
    }

    private StockEntry entry(long id, OffsetDateTime expiresAt) {
        Product product = new Product();
        product.setName("Молоко");
        StockEntry entry = new StockEntry();
        ReflectionTestUtils.setField(entry, "id", id);
        entry.setProduct(product);
        entry.setStatus(StockStatus.AVAILABLE);
        entry.setAddedAt(now.minusDays(1));
        entry.setExpiresAt(expiresAt);
        return entry;
    }

    @Test
    void expiredItemGetsExpiredNotificationNotExpiringSoon() {
        when(stockEntryRepository.findWithProductByStatus(StockStatus.AVAILABLE))
            .thenReturn(List.of(entry(1L, OffsetDateTime.of(2026, 10, 17, 0, 0, 0, 0, ZoneOffset.UTC))));

        scheduler.scan(now);

        verify(notificationService).createIfMissing(eq("expired:1"), eq("EXPIRED"), anyString(),
            eq("Молоко: срок годности истёк 17.10.2026"), eq("StockEntry"), eq("1"));
        verify(notificationService, never()).createIfMissing(startsWith("expiring:"), any(), any(), any(), any(), any());
    }

    @Test
    void itemExpiringTodayIsExpiringSoon() {
        when(stockEntryRepository.findWithProductByStatus(StockStatus.AVAILABLE))
            .thenReturn(List.of(entry(2L, OffsetDateTime.of(2026, 10, 19, 0, 0, 0, 0, ZoneOffset.UTC))));

        scheduler.scan(now);

        verify(notificationService).createIfMissing(eq("expiring:2"), eq("EXPIRING_SOON"), anyString(),
            eq("Молоко: годен до 19.10.2026"), any(), any());
    }

    @Test
    void disabledSettingsSuppressNotifications() {
        settings.setNotifyExpired(false);
        settings.setNotifyExpiring(false);
        when(stockEntryRepository.findWithProductByStatus(StockStatus.AVAILABLE)).thenReturn(List.of(
            entry(1L, now.minusDays(3)),
            entry(2L, now)));

        scheduler.scan(now);

        verify(notificationService, never()).createIfMissing(any(), any(), any(), any(), any(), any());
    }

    @Test
    void nothingIsCreatedDuringQuietHours() {
        settings.setQuietHoursStart("11:00");
        settings.setQuietHoursEnd("13:00");

        scheduler.scan(now);

        verifyNoInteractions(stockEntryRepository);
    }

    @Test
    void quietHoursWrapAroundMidnight() {
        settings.setQuietHoursStart("22:00");
        settings.setQuietHoursEnd("07:00");

        assertTrue(NotificationService.isQuietTime(settings, LocalTime.of(23, 30)));
        assertTrue(NotificationService.isQuietTime(settings, LocalTime.of(6, 59)));
        assertFalse(NotificationService.isQuietTime(settings, LocalTime.of(7, 0)));
        assertFalse(NotificationService.isQuietTime(settings, LocalTime.of(12, 0)));
    }
}
