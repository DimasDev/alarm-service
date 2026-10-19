package com.example.alarmservice.service;

import com.example.alarmservice.BaseIntegrationTest;
import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.dto.AlarmIngestionResult;
import com.example.alarmservice.exception.UnknownAlarmException;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import com.example.alarmservice.model.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class AlarmServiceIntegrationTest extends BaseIntegrationTest {

    private AlarmEventRequest event(String eventId, AlarmStatus status, AlarmSeverity severity) {

        return new AlarmEventRequest(
                eventId,
                ELEMENT_ID_1,
                ALARM_CODE_CELL_DOWN,
                severity,
                status,
                1791800000000L,
                TEST_ALARM
        );
    }

    @Test
    void raisedEventShouldCreateHistoryAndActiveAlarm() {

        AlarmIngestionResult result = alarmIngestionService.processAlarm(event(EVENT_ID_1, AlarmStatus.RAISED, AlarmSeverity.CRITICAL));

        assertTrue(result.created());
        assertEquals(1, historyRepository.count());
        assertEquals(1, activeRepository.count());
    }

    @Test
    void repeatedRaisedShouldUpdateActiveAlarm() {

        alarmIngestionService.processAlarm(event(EVENT_ID_1, AlarmStatus.RAISED, AlarmSeverity.CRITICAL));

        alarmIngestionService.processAlarm(event(EVENT_ID_2, AlarmStatus.RAISED, AlarmSeverity.MAJOR));

        assertEquals(2, historyRepository.count());
        assertEquals(1, activeRepository.count());

        var active = activeRepository.findByElementIdAndAlarmCode(ELEMENT_ID_1, ALARM_CODE_CELL_DOWN).orElseThrow();

        assertEquals(AlarmSeverity.MAJOR, active.getSeverity());
    }

    @Test
    void clearedShouldRemoveActiveButKeepHistory() {

        alarmIngestionService.processAlarm(event(EVENT_ID_1, AlarmStatus.RAISED, AlarmSeverity.CRITICAL));

        alarmIngestionService.processAlarm(event(EVENT_ID_2, AlarmStatus.CLEARED, AlarmSeverity.CRITICAL));

        assertEquals(2, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void duplicateEventShouldNotBeProcessedTwice() {

        var request = event(EVENT_ID_1, AlarmStatus.RAISED, AlarmSeverity.CRITICAL);

        assertTrue(alarmIngestionService.processAlarm(request).created());
        AlarmIngestionResult result = alarmIngestionService.processAlarm(request);
        assertFalse(result.created());
        assertEquals(NotificationStatus.DUPLICATE, result.notificationStatus());

        assertEquals(1, historyRepository.count());
        assertEquals(1, activeRepository.count());
    }

    @Test
    void clearingUnknownAlarmShouldThrowException() {

        assertThrows(
                UnknownAlarmException.class,
                () -> alarmIngestionService.processAlarm(event(EVENT_ID_1, AlarmStatus.CLEARED, AlarmSeverity.CRITICAL))
        );

        assertEquals(0, historyRepository.count());
    }

    @Test
    void concurrentDuplicateEventsShouldBeProcessedOnce() throws Exception {

        var request = event(EVENT_ID_1, AlarmStatus.RAISED, AlarmSeverity.CRITICAL);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<AlarmIngestionResult> task = () -> {
                start.await();
                return alarmIngestionService.processAlarm(request);
            };

            Future<AlarmIngestionResult> first = executor.submit(task);
            Future<AlarmIngestionResult> second = executor.submit(task);

            start.countDown();

            AlarmIngestionResult result1 = first.get(10, TimeUnit.SECONDS);
            AlarmIngestionResult result2 = second.get(10, TimeUnit.SECONDS);

            assertNotEquals(result1.created(), result2.created());
            assertEquals(1, historyRepository.count());
            assertEquals(1, activeRepository.count());

        } finally {
            executor.shutdownNow();
        }
    }
}
