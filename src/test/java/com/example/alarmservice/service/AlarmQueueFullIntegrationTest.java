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

import static com.example.alarmservice.exception.ErrorMessages.TIMED_OUT_WAITING_FOR_RELEASE;
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

    public static final String FULL_1 = "FULL-1";
    public static final String FULL_2 = "FULL-2";
    public static final String FULL_3 = "FULL-3";
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
                throw new IllegalStateException(TIMED_OUT_WAITING_FOR_RELEASE);
            }

            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        try {
            AlarmIngestionResult first = alarmIngestionService.processAlarm(alarm(FULL_1));

            assertTrue(first.created());
            assertEquals(NotificationStatus.QUEUED, first.notificationStatus());

            assertTrue(started.await(5, TimeUnit.SECONDS));

            AlarmIngestionResult second = alarmIngestionService.processAlarm(alarm(FULL_2));

            assertTrue(second.created());
            assertEquals(NotificationStatus.QUEUED, second.notificationStatus());

            AlarmIngestionResult third = alarmIngestionService.processAlarm(alarm(FULL_3));

            assertTrue(third.created());
            assertEquals(NotificationStatus.QUEUE_FULL, third.notificationStatus());

            assertTrue(historyRepository.existsByEventId(FULL_1));
            assertTrue(historyRepository.existsByEventId(FULL_2));
            assertTrue(historyRepository.existsByEventId(FULL_3));

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
                throw new IllegalStateException(TIMED_OUT_WAITING_FOR_RELEASE);
            }

            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        try {
            AlarmIngestionResult first = alarmIngestionService.processAlarm(alarm(FULL_1));

            assertEquals(NotificationStatus.QUEUED, first.notificationStatus());
            assertTrue(started.await(5, TimeUnit.SECONDS));

            AlarmIngestionResult second = alarmIngestionService.processAlarm(alarm(FULL_2));

            assertEquals(NotificationStatus.QUEUED, second.notificationStatus());

            AlarmIngestionResult third = alarmIngestionService.processAlarm(alarm(FULL_3));

            assertTrue(third.created());
            assertEquals(NotificationStatus.QUEUE_FULL, third.notificationStatus());

            AlarmIngestionResult duplicate = alarmIngestionService.processAlarm(alarm(FULL_3));

            assertFalse(duplicate.created());
            assertEquals(NotificationStatus.DUPLICATE, duplicate.notificationStatus());

            assertEquals(3, historyRepository.count());
            assertEquals(1, activeRepository.count());

            verify(httpClient, never()).send(eq(FULL_3), anyString(), any());

        } finally {
            release.countDown();
        }
    }
}
