package com.example.alarmservice.service;

import com.example.alarmservice.BaseIntegrationTest;
import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import com.example.alarmservice.notification.NotificationHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

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
    void shouldRejectAlarmWithoutPersistingWhenQueueIsFull() throws Exception {

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
            assertTrue(alarmIngestionService.processAlarm(
                    alarm("FULL-1")
            ));

            assertTrue(started.await(5, TimeUnit.SECONDS));

            assertTrue(alarmIngestionService.processAlarm(
                    alarm("FULL-2")
            ));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> alarmIngestionService.processAlarm(
                            alarm("FULL-3")
                    )
            );

            assertEquals(503, exception.getStatusCode().value());

            // Третье событие не должно попасть в БД
            assertFalse(historyRepository.existsByEventId("FULL-3"));

            // Первые два успешно сохранены
            assertTrue(historyRepository.existsByEventId("FULL-1"));
            assertTrue(historyRepository.existsByEventId("FULL-2"));

        } finally {
            release.countDown();
        }
    }
}
