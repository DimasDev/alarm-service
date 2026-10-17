package com.example.alarmservice;

import com.example.alarmservice.model.AlarmSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.example.alarmservice.exception.ErrorMessages.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AlarmControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn201WhenAlarmCreatedAnd200WhenAlarmExists() throws Exception {
        String requestBody = """
                {
                    "eventId": "EVT-10001",
                    "elementId": "BTS-102",
                    "alarmCode": "CELL_DOWN",
                    "severity": "CRITICAL",
                    "status": "RAISED",
                    "timestamp": 1791800000000,
                    "description": "Cell is not responding"
                }
                """;

        mockMvc.perform(post(API_ALARMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());
        mockMvc.perform(post(API_ALARMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        assertEquals(1, historyRepository.count());
        assertEquals(1, activeRepository.count());
    }

    @Test
    void shouldReturn400WhenSeverityNotValid() throws Exception {
        String requestBody = """
                {
                    "eventId": "EVT-10001",
                    "elementId": "BTS-102",
                    "alarmCode": "CELL_DOWN",
                    "severity": "ZERO",
                    "status": "RAISED",
                    "timestamp": 1791800000000,
                    "description": "Cell is not responding"
                }
                """;

        mockMvc.perform(post(API_ALARMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertEquals(0, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void shouldReturn400WhenStatusNotValid() throws Exception {
        String requestBody = """
                {
                    "eventId": "EVT-10001",
                    "elementId": "BTS-102",
                    "alarmCode": "CELL_DOWN",
                    "severity": "MAJOR",
                    "status": "ACTIVE",
                    "timestamp": 1791800000000,
                    "description": "Cell is not responding"
                }
                """;

        mockMvc.perform(post(API_ALARMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertEquals(0, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void shouldReturn400WhenElementIdNotValid() throws Exception {
        String requestBody = """
                {
                    "eventId": "EVT-10001",
                    "elementId": "",
                    "alarmCode": "CELL_DOWN",
                    "severity": "ZERO",
                    "status": "RAISED",
                    "timestamp": 1791800000000,
                    "description": "Cell is not responding"
                }
                """;

        mockMvc.perform(post(API_ALARMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertEquals(0, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void shouldReturn400WhenAlarmCodeNotValid() throws Exception {
        String requestBody = """
                {
                    "eventId": "EVT-10001",
                    "elementId": "BTS-102",
                    "alarmCode": null,
                    "severity": "ZERO",
                    "status": "RAISED",
                    "timestamp": 1791800000000,
                    "description": "Cell is not responding"
                }
                """;

        mockMvc.perform(post(API_ALARMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        assertEquals(0, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void shouldReturn409WhenClearingNotExistedAlarm() throws Exception {
        String requestBody = """
                {
                    "eventId": "EVT-10001",
                    "elementId": "BTS-100",
                    "alarmCode": "CELL_DOWN",
                    "severity": "CRITICAL",
                    "status": "CLEARED",
                    "timestamp": 1791800000000,
                    "description": "Cell is not responding"
                }
                """;

        mockMvc.perform(post(API_ALARMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(ALARM_NOT_FOUND.formatted(ELEMENT_ID_1, ALARM_CODE_CELL_DOWN)));

        assertEquals(0, historyRepository.count());
        assertEquals(0, activeRepository.count());
    }

    @Test
    void shouldReturn200AndActiveAlarms() throws Exception {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        mockMvc.perform(get("/api/alarms/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].elementId").value(ELEMENT_ID_1))
                .andExpect(jsonPath("$[0].alarmCode").value(ALARM_CODE_CELL_DOWN));
    }

    @Test
    void shouldReturn200AndActiveAlarmsForSpecificElement() throws Exception {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        mockMvc.perform(get("/api/alarms/elements/BTS-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alarmCode").value(ALARM_CODE_CELL_DOWN));
    }

    @Test
    void shouldReturn200AndActiveAlarmCountsBySeverity() throws Exception {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        mockMvc.perform(get("/api/alarms/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.CRITICAL").value(1))
                .andExpect(jsonPath("$.MAJOR").value(0))
                .andExpect(jsonPath("$.WARNING").value(0))
                .andExpect(jsonPath("$.INFO").value(0));
    }

    @Test
    void shouldReturn200AndAlarmHistory() throws Exception {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.CRITICAL, 1000L);

        mockMvc.perform(get(API_ALARMS_HISTORY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("RAISED"));
    }

    @Test
    void shouldReturn200AndSupportPaginationAndSorting() throws Exception {
        raise(EVENT_ID_1, ELEMENT_ID_1, ALARM_CODE_CELL_DOWN,
                AlarmSeverity.INFO, 1000L);

        raise(EVENT_ID_2, ELEMENT_ID_1, ALARM_CODE_HIGH_CPU,
                AlarmSeverity.WARNING, 2000L);

        raise(EVENT_ID_3, ELEMENT_ID_1, ALARM_CODE_POWER_FAILURE,
                AlarmSeverity.MAJOR, 3000L);

        mockMvc.perform(get(API_ALARMS_HISTORY)
                        .param("page", "1")
                        .param("size", "1")
                        .param("sortBy", "timestamp")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].eventId").value(EVENT_ID_2));
    }

    @Test
    void shouldReturn400AndRejectInvalidPageNumber() throws Exception {

        mockMvc.perform(get(API_ALARMS_HISTORY)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(PAGE_NEGATIVE));
    }

    @Test
    void shouldReturn400AndRejectInvalidSizeLessThen() throws Exception {

        mockMvc.perform(get(API_ALARMS_HISTORY)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(SIZE_INVALID));
    }

    @Test
    void shouldReturn400AndRejectInvalidSizeGreaterThen() throws Exception {

        mockMvc.perform(get(API_ALARMS_HISTORY)
                        .param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(SIZE_INVALID));
    }

    @Test
    void shouldReturn400AndRejectInvalidSortByValue() throws Exception {

        mockMvc.perform(get(API_ALARMS_HISTORY)
                        .param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(SORT_INVALID.formatted("password")));
    }

    @Test
    void shouldReturn400AndRejectInvalidSortDirection() throws Exception {

        mockMvc.perform(get(API_ALARMS_HISTORY)
                        .param("direction", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(DIRECTION_INVALID));
    }

    @Test
    void shouldReturn400AndRejectNonNumericPage() throws Exception {
        mockMvc.perform(get(API_ALARMS_HISTORY)
                        .param("page", "abc"))
                .andExpect(status().isBadRequest());
    }
}
