package com.example.alarmservice.service;

import com.example.alarmservice.BaseIntegrationTest;
import com.example.alarmservice.dto.ActiveAlarmResponse;
import com.example.alarmservice.dto.AlarmHistoryResponse;
import com.example.alarmservice.model.AlarmSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlarmQueryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AlarmQueryService queryService;

    @Test
    void shouldReturnOnlyActiveAlarms() {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        raise(EVENT_ID_2, ELEMENT_ID_2, ALARM_CODE_HIGH_CPU,
                AlarmSeverity.MAJOR, 2000L);

        clear(EVENT_ID_3, ELEMENT_ID_2, ALARM_CODE_HIGH_CPU, 3000L);

        List<ActiveAlarmResponse> result =
                queryService.getActiveAlarms();

        assertEquals(1, result.size());
        assertEquals(ELEMENT_ID_1, result.getFirst().elementId());
    }

    @Test
    void shouldFilterActiveAlarmsByElement() {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        raise(EVENT_ID_2, ELEMENT_ID_2, ALARM_CODE_HIGH_CPU,
                AlarmSeverity.MAJOR, 2000L);

        clear(EVENT_ID_3, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN, 3000L);

        List<ActiveAlarmResponse> result =
                queryService.getActiveAlarmsByElement(ELEMENT_ID_2);

        assertEquals(1, result.size());
        assertEquals(ELEMENT_ID_2, result.getFirst().elementId());
    }

    @Test
    void shouldReturnActiveAlarmCountsBySeverity() throws Exception {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        raise(EVENT_ID_2, ELEMENT_ID_2, ALARM_CODE_HIGH_CPU,
                AlarmSeverity.CRITICAL, 2000L);

        raise(EVENT_ID_3, ELEMENT_ID_3, ALARM_CODE_POWER_FAILURE,
                AlarmSeverity.MAJOR, 3000L);

        clear(EVENT_ID_4, ELEMENT_ID_2, ALARM_CODE_HIGH_CPU, 4000L);

        Map<AlarmSeverity, Long> result = queryService.getStatistics();
        assertEquals(1L, result.get(AlarmSeverity.CRITICAL));
        assertEquals(1L, result.get(AlarmSeverity.MAJOR));
        assertEquals(0L, result.get(AlarmSeverity.WARNING));
        assertEquals(0L, result.get(AlarmSeverity.INFO));
    }

    @Test
    void shouldReturnCorrectHistory() {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        raise(EVENT_ID_2, ELEMENT_ID_2, ALARM_CODE_HIGH_CPU,
                AlarmSeverity.WARNING, 2000L);

        clear(EVENT_ID_3, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN, 3000L);

        Page<AlarmHistoryResponse> result = queryService.getHistory(null, 0, 10, Sort.Direction.DESC, ID);
        assertEquals(3, result.getTotalElements());
    }

    @Test
    void shouldFilterHistoryByElementId() {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        raise(EVENT_ID_2, ELEMENT_ID_2, ALARM_CODE_HIGH_CPU,
                AlarmSeverity.WARNING, 2000L);

        Page<AlarmHistoryResponse> result = queryService.getHistory(ELEMENT_ID_1, 0, 10, Sort.Direction.DESC, ID);
        assertEquals(1, result.getTotalElements());
        assertEquals(ELEMENT_ID_1, result.getContent().getFirst().elementId());
    }

    @Test
    void shouldSupportPaginationAndSorting() {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.INFO, 1000L);

        raise(EVENT_ID_2, ELEMENT_ID_1, ALARM_CODE_HIGH_CPU,
                AlarmSeverity.WARNING, 2000L);

        raise(EVENT_ID_3, ELEMENT_ID_1, ALARM_CODE_POWER_FAILURE,
                AlarmSeverity.MAJOR, 3000L);

        Page<AlarmHistoryResponse> result = queryService.getHistory(null, 0, 1, Sort.Direction.ASC, TIMESTAMP);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getContent().size());
        assertEquals(EVENT_ID_1, result.getContent().getFirst().eventId());

        result = queryService.getHistory(null, 1, 1, Sort.Direction.ASC, TIMESTAMP);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getContent().size());
        assertEquals(EVENT_ID_2, result.getContent().getFirst().eventId());

        result = queryService.getHistory(null, 2, 1, Sort.Direction.ASC, TIMESTAMP);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getContent().size());
        assertEquals(EVENT_ID_3, result.getContent().getFirst().eventId());
    }

}
