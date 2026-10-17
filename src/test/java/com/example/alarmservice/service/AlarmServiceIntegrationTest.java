package com.example.alarmservice.service;

import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.exception.UnknownAlarmException;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import com.example.alarmservice.repository.ActiveAlarmRepository;
import com.example.alarmservice.repository.AlarmHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class AlarmServiceIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AlarmHistoryRepository alarmHistoryRepository;

    @Autowired
    private ActiveAlarmRepository activeRepository;

    @BeforeEach
    void cleanDatabase() {
        alarmHistoryRepository.deleteAll();
        activeRepository.deleteAll();
    }

    private AlarmEventRequest event(
            String eventId,
            AlarmStatus status,
            AlarmSeverity severity) {

        return new AlarmEventRequest(
                eventId,
                "BTS-102",
                "CELL_DOWN",
                severity,
                status,
                1791800000000L,
                "Test alarm"
        );
    }

    @Test
    void raisedEventShouldCreateHistoryAndActiveAlarm() {

        boolean created = alarmService.processAlarm(
                event("EVT-1", AlarmStatus.RAISED,
                        AlarmSeverity.CRITICAL)
        );

        assertTrue(created);
        assertEquals(1, alarmHistoryRepository.count());
        assertEquals(1, activeRepository.count());
    }

    @Test
    void repeatedRaisedShouldUpdateActiveAlarm() {

        alarmService.processAlarm(
                event("EVT-1", AlarmStatus.RAISED,
                        AlarmSeverity.CRITICAL)
        );

        alarmService.processAlarm(
                event("EVT-2", AlarmStatus.RAISED,
                        AlarmSeverity.MAJOR)
        );

        assertEquals(2, alarmHistoryRepository.count());
        assertEquals(1, activeRepository.count());

        var active = activeRepository
                .findByElementIdAndAlarmCode(
                        "BTS-102", "CELL_DOWN"
                ).orElseThrow();

        assertEquals(AlarmSeverity.MAJOR, active.getSeverity());
    }

    @Test
    void clearedShouldRemoveActiveButKeepHistory() {

        alarmService.processAlarm(
                event("EVT-1", AlarmStatus.RAISED,
                        AlarmSeverity.CRITICAL)
        );

        alarmService.processAlarm(
                event("EVT-2", AlarmStatus.CLEARED,
                        AlarmSeverity.CRITICAL)
        );

        assertEquals(2, alarmHistoryRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void duplicateEventShouldNotBeProcessedTwice() {

        var request = event(
                "EVT-1", AlarmStatus.RAISED,
                AlarmSeverity.CRITICAL
        );

        assertTrue(alarmService.processAlarm(request));
        assertFalse(alarmService.processAlarm(request));

        assertEquals(1, alarmHistoryRepository.count());
        assertEquals(1, activeRepository.count());
    }

    @Test
    void clearingUnknownAlarmShouldThrowException() {

        assertThrows(
                UnknownAlarmException.class,
                () -> alarmService.processAlarm(
                        event("EVT-1", AlarmStatus.CLEARED,
                                AlarmSeverity.CRITICAL)
                )
        );

        assertEquals(0, alarmHistoryRepository.count());
    }

    @Test
    void concurrentDuplicateEventsShouldBeProcessedOnce()
            throws Exception {

        var request = event(
                "EVT-1", AlarmStatus.RAISED,
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
            assertEquals(1, alarmHistoryRepository.count());
            assertEquals(1, activeRepository.count());

        } finally {
            executor.shutdownNow();
        }
    }
}
