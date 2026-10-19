package com.example.alarmservice.service;

import com.example.alarmservice.BaseIntegrationTest;
import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static com.example.alarmservice.exception.ErrorMessages.NOTIFICATION_SERIALIZATION_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AlarmServiceRollbackIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private JsonMapper objectMapper;

    @Test
    void shouldNotPersistAlarmWhenNotificationSerializationFails() {

        AlarmEventRequest request = new AlarmEventRequest(
                "EVT-ROLLBACK-1",
                "BTS-100",
                "CELL_DOWN",
                AlarmSeverity.CRITICAL,
                AlarmStatus.RAISED,
                1791800000000L,
                "Test alarm"
        );

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JacksonException("Serialization failed") {
                });

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> alarmIngestionService.processAlarm(request)
        );

        assertEquals(
                NOTIFICATION_SERIALIZATION_FAILED,
                exception.getMessage()
        );

        assertEquals(0, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }
}
