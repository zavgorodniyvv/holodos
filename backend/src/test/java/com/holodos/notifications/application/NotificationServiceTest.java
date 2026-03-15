package com.holodos.notifications.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.holodos.notifications.domain.Notification;
import com.holodos.notifications.infrastructure.NotificationRepository;
import com.holodos.settings.application.UserSettingsService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository notificationRepository;
    @Mock UserSettingsService userSettingsService;

    NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, userSettingsService);
    }

    @Test
    void createIfMissingIsDeduplicated() {
        Notification existing = new Notification();
        when(notificationRepository.findByDedupKey("k1")).thenReturn(Optional.of(existing));

        Notification n = service.createIfMissing("k1", "EXPIRING_SOON", "t", "m", "StockEntry", "1");

        assertEquals(existing, n);
        verify(notificationRepository, never()).save(any());
    }
}
