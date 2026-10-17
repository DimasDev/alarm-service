package com.example.alarmservice.dto;

import com.example.alarmservice.entity.AlarmHistory;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;

public record AlarmHistoryResponse(String eventId,
                                   String elementId,
                                   String alarmCode,
                                   AlarmSeverity severity,
                                   AlarmStatus status,
                                   Long timestamp,
                                   String description) {

    public static AlarmHistoryResponse from(AlarmHistory alarmHistory) {
        return new AlarmHistoryResponse(alarmHistory.getEventId(),
                alarmHistory.getElementId(),
                alarmHistory.getAlarmCode(),
                alarmHistory.getSeverity(),
                alarmHistory.getStatus(),
                alarmHistory.getTimestamp(),
                alarmHistory.getDescription());
    }
}
