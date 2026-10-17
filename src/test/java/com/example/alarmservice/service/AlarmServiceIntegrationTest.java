package com.example.alarmservice.service;

import com.example.alarmservice.BaseIntegrationTest;
import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.exception.UnknownAlarmException;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class AlarmServiceIntegrationTest extends BaseIntegrationTest {

    private AlarmEventRequest event(
            String eventId,
            AlarmStatus status,
            AlarmSeverity severity) {

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

        boolean created = alarmService.processAlarm(
                event(EVENT_ID_1, AlarmStatus.RAISED,
                        AlarmSeverity.CRITICAL)
        );

        assertTrue(created);
        assertEquals(1, historyRepository.count());
        assertEquals(1, activeRepository.count());
    }

    @Test
    void repeatedRaisedShouldUpdateActiveAlarm() {

        alarmService.processAlarm(
                event(EVENT_ID_1, AlarmStatus.RAISED,
                        AlarmSeverity.CRITICAL)
        );

        alarmService.processAlarm(
                event(EVENT_ID_2, AlarmStatus.RAISED,
                        AlarmSeverity.MAJOR)
        );

        assertEquals(2, historyRepository.count());
        assertEquals(1, activeRepository.count());

        var active = activeRepository
                .findByElementIdAndAlarmCode(
                        ELEMENT_ID_1, ALARM_CODE_CELL_DOWN
                ).orElseThrow();

        assertEquals(AlarmSeverity.MAJOR, active.getSeverity());
    }

    @Test
    void clearedShouldRemoveActiveButKeepHistory() {

        alarmService.processAlarm(
                event(EVENT_ID_1, AlarmStatus.RAISED,
                        AlarmSeverity.CRITICAL)
        );

        alarmService.processAlarm(
                event(EVENT_ID_2, AlarmStatus.CLEARED,
                        AlarmSeverity.CRITICAL)
        );

        assertEquals(2, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void duplicateEventShouldNotBeProcessedTwice() {

        var request = event(
                EVENT_ID_1, AlarmStatus.RAISED,
                AlarmSeverity.CRITICAL
        );

        assertTrue(alarmService.processAlarm(request));
        assertFalse(alarmService.processAlarm(request));

        assertEquals(1, historyRepository.count());
        assertEquals(1, activeRepository.count());
    }

    @Test
    void clearingUnknownAlarmShouldThrowException() {

        assertThrows(
                UnknownAlarmException.class,
                () -> alarmService.processAlarm(
                        event(EVENT_ID_1, AlarmStatus.CLEARED,
                                AlarmSeverity.CRITICAL)
                )
        );

        assertEquals(0, historyRepository.count());
    }

    @Test
    void concurrentDuplicateEventsShouldBeProcessedOnce()
            throws Exception {

        var request = event(
                EVENT_ID_1, AlarmStatus.RAISED,
                AlarmSeverity.CRITICAL
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Boolean> task = () -> {
                start.await();
                return alarmService.processAlarm(request);
            };

            Future<Boolean> first = executor.submit(task);
            Future<Boolean> second = executor.submit(task);

            start.countDown();

            boolean result1 = first.get(10, TimeUnit.SECONDS);
            boolean result2 = second.get(10, TimeUnit.SECONDS);

            assertNotEquals(result1, result2);
            assertEquals(1, historyRepository.count());
            assertEquals(1, activeRepository.count());

        } finally {
            executor.shutdownNow();
        }
    }
}
