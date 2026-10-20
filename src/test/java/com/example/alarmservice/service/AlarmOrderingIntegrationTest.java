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

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.alarmservice.exception.ErrorMessages.TIMED_OUT_WAITING_FOR_RELEASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {
        "notification.enabled=true",
        "notification.workers=2",
        "notification.queue-capacity=10"
})
public class AlarmOrderingIntegrationTest extends BaseIntegrationTest {

    private static final String ORDER_1 = "ORDER-1";
    private static final String ORDER_2 = "ORDER-2";
    private static final String ORDER_3 = "ORDER-3";

    @MockitoBean
    private NotificationHttpClient httpClient;

    private AlarmEventRequest alarm(String eventId, String elementId) {
        return new AlarmEventRequest(
                eventId,
                elementId,
                ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL,
                AlarmStatus.RAISED,
                1791800000000L,
                TEST_ALARM
        );
    }

    @Test
    void shouldPreserveNotificationOrderForSameElement() throws Exception {

        List<String> received = new CopyOnWriteArrayList<>();
        CountDownLatch delivered = new CountDownLatch(3);

        doAnswer(invocation -> {
            received.add(invocation.getArgument(0));
            delivered.countDown();
            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        AlarmIngestionResult result = alarmIngestionService.processAlarm(alarm(ORDER_1, ELEMENT_ID_1));
        assertTrue(result.created());
        assertEquals(NotificationStatus.QUEUED, result.notificationStatus());

        result = alarmIngestionService.processAlarm(alarm(ORDER_2, ELEMENT_ID_1));
        assertTrue(result.created());
        assertEquals(NotificationStatus.QUEUED, result.notificationStatus());

        result = alarmIngestionService.processAlarm(alarm(ORDER_3, ELEMENT_ID_1));
        assertTrue(result.created());
        assertEquals(NotificationStatus.QUEUED, result.notificationStatus());

        assertTrue(delivered.await(5, TimeUnit.SECONDS));

        assertEquals(List.of(ORDER_1, ORDER_2, ORDER_3), received);
    }

    @Test
    void shouldNotSendSameElementNotificationsConcurrently() throws Exception {

        AtomicInteger activeRequests = new AtomicInteger();
        AtomicInteger maxConcurrentRequests = new AtomicInteger();

        CountDownLatch delivered = new CountDownLatch(6);

        doAnswer(invocation -> {
            int active = activeRequests.incrementAndGet();
            maxConcurrentRequests.accumulateAndGet(active, Math::max);

            try {
                Thread.sleep(50);
            } finally {
                activeRequests.decrementAndGet();
                delivered.countDown();
            }

            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        ExecutorService executor = Executors.newFixedThreadPool(6);

        try {
            List<Future<AlarmIngestionResult>> futures = new java.util.ArrayList<>();

            for (int i = 1; i <= 6; i++) {
                int index = i;

                futures.add(executor.submit(() -> alarmIngestionService.processAlarm(alarm("CONCURRENT-" + index, ELEMENT_ID_1))));
            }

            for (Future<AlarmIngestionResult> future : futures) {
                AlarmIngestionResult result = future.get(10, TimeUnit.SECONDS);

                assertTrue(result.created());
                assertEquals(NotificationStatus.QUEUED, result.notificationStatus());
            }

            assertTrue(delivered.await(10, TimeUnit.SECONDS));

            assertEquals(1, maxConcurrentRequests.get());

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldProcessDifferentElementsConcurrently() throws Exception {

        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);

        doAnswer(invocation -> {
            bothStarted.countDown();

            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(TIMED_OUT_WAITING_FOR_RELEASE);
            }

            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        try {
            AlarmIngestionResult result = alarmIngestionService.processAlarm(alarm("PARALLEL-1", ELEMENT_ID_1));
            assertTrue(result.created());
            assertEquals(NotificationStatus.QUEUED, result.notificationStatus());

            result = alarmIngestionService.processAlarm(alarm("PARALLEL-2", ELEMENT_ID_1));
            assertTrue(result.created());
            assertEquals(NotificationStatus.QUEUED, result.notificationStatus());

            assertTrue(bothStarted.await(5, TimeUnit.SECONDS), "Different elements should be processed concurrently");

        } finally {
            release.countDown();
        }
    }
}
