package com.example.alarmservice.service;

import com.example.alarmservice.BaseIntegrationTest;
import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.dto.AlarmIngestionResult;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import com.example.alarmservice.model.NotificationStatus;
import com.example.alarmservice.notification.NotificationHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "notification.enabled=true",
        "notification.workers=1",
        "notification.queue-capacity=2"
})
public class AlarmQueueFullIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private NotificationHttpClient httpClient;

    private AlarmEventRequest alarm(String eventId) {
        return new AlarmEventRequest(
                eventId,
                ELEMENT_ID_1,
                ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL,
                AlarmStatus.RAISED,
                1791800000000L,
                TEST_ALARM
        );
    }

    @Test
    void shouldPersistAlarmWhenNotificationQueueIsFull() throws Exception {

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        doAnswer(invocation -> {
            started.countDown();

            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for release");
            }

            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        try {
            AlarmIngestionResult first = alarmIngestionService.processAlarm(alarm("FULL-1"));

            assertTrue(first.created());
            assertEquals(NotificationStatus.QUEUED, first.notificationStatus());

            assertTrue(started.await(5, TimeUnit.SECONDS));

            AlarmIngestionResult second = alarmIngestionService.processAlarm(alarm("FULL-2"));

            assertTrue(second.created());
            assertEquals(NotificationStatus.QUEUED, second.notificationStatus());

            AlarmIngestionResult third = alarmIngestionService.processAlarm(alarm("FULL-3"));

            assertTrue(third.created());
            assertEquals(NotificationStatus.QUEUE_FULL, third.notificationStatus());

            assertTrue(historyRepository.existsByEventId("FULL-1"));
            assertTrue(historyRepository.existsByEventId("FULL-2"));
            assertTrue(historyRepository.existsByEventId("FULL-3"));

            assertEquals(3, historyRepository.count());
            assertEquals(1, activeRepository.count());

        } finally {
            release.countDown();
        }
    }

    @Test
    void shouldNotRetryNotificationForDuplicateAlarmAfterQueueFull() throws Exception {

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        doAnswer(invocation -> {
            started.countDown();

            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for release");
            }

            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        try {
            // First notification occupies the worker.
            AlarmIngestionResult first =
                    alarmIngestionService.processAlarm(alarm("FULL-1"));

            assertEquals(NotificationStatus.QUEUED, first.notificationStatus());
            assertTrue(started.await(5, TimeUnit.SECONDS));

            // Second notification occupies the remaining capacity.
            AlarmIngestionResult second =
                    alarmIngestionService.processAlarm(alarm("FULL-2"));

            assertEquals(NotificationStatus.QUEUED, second.notificationStatus());

            // Third alarm is persisted, but its notification is skipped.
            AlarmIngestionResult third =
                    alarmIngestionService.processAlarm(alarm("FULL-3"));

            assertTrue(third.created());
            assertEquals(NotificationStatus.QUEUE_FULL, third.notificationStatus());

            // Retry the same eventId while the queue is still full.
            AlarmIngestionResult duplicate =
                    alarmIngestionService.processAlarm(alarm("FULL-3"));

            assertFalse(duplicate.created());
            assertEquals(NotificationStatus.DUPLICATE, duplicate.notificationStatus());

            // No additional history or active alarm records.
            assertEquals(3, historyRepository.count());
            assertEquals(1, activeRepository.count());

            // The duplicate must not trigger another notification.
            verify(httpClient, never()).send(
                    eq("FULL-3"),
                    anyString(),
                    any()
            );

        } finally {
            release.countDown();
        }
    }
}
