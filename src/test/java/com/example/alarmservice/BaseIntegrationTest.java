package com.example.alarmservice;

import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import com.example.alarmservice.repository.ActiveAlarmRepository;
import com.example.alarmservice.repository.AlarmHistoryRepository;
import com.example.alarmservice.service.AlarmIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
public abstract class BaseIntegrationTest {

    protected static final String EVENT_ID_1 = "EVT-1";
    protected static final String EVENT_ID_2 = "EVT-2";
    protected static final String EVENT_ID_3 = "EVT-3";
    protected static final String EVENT_ID_4 = "EVT-4";
    public static final String ELEMENT_ID_1 = "BTS-100";
    protected static final String ELEMENT_ID_2 = "BTS-200";
    protected static final String ELEMENT_ID_3 = "BTS-300";
    protected static final String ALARM_CODE_CELL_DOWN = "CELL_DOWN";
    protected static final String ALARM_CODE_HIGH_CPU = "HIGH_CPU";
    protected static final String ALARM_CODE_POWER_FAILURE = "POWER_FAILURE";
    protected static final String TEST_ALARM = "Test alarm";
    protected static final String TIMESTAMP = "timestamp";
    protected static final String ID = "id";
    protected static final String API_ALARMS = "/api/alarms";
    protected static final String API_ALARMS_HISTORY = "/api/alarms/history";

    @Autowired
    protected AlarmIngestionService alarmIngestionService;

    @Autowired
    protected ActiveAlarmRepository activeRepository;

    @Autowired
    protected AlarmHistoryRepository historyRepository;

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void setUp() {
        activeRepository.deleteAll();
        historyRepository.deleteAll();
    }

    protected void raise(String eventId, String elementId, String alarmCode, AlarmSeverity severity, long timestamp) {

        alarmIngestionService.processAlarm(new AlarmEventRequest(
                eventId,
                elementId,
                alarmCode,
                severity,
                AlarmStatus.RAISED,
                timestamp,
                "Test alarm"
        ));
    }

    protected void clear(String eventId, String elementId, String alarmCode, long timestamp) {

        alarmIngestionService.processAlarm(new AlarmEventRequest(
                eventId,
                elementId,
                alarmCode,
                AlarmSeverity.INFO,
                AlarmStatus.CLEARED,
                timestamp,
                "Cleared"
        ));
    }
}
