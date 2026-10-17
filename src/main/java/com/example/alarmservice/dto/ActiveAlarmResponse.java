package com.example.alarmservice.dto;

import com.example.alarmservice.entity.ActiveAlarm;
import com.example.alarmservice.model.AlarmSeverity;

public record ActiveAlarmResponse(String elementId, String alarmCode, AlarmSeverity severity, Long timestamp,
                                  String description) {
    public static ActiveAlarmResponse from(ActiveAlarm alarm) {
        return new ActiveAlarmResponse(alarm.getElementId(),
                alarm.getAlarmCode(),
                alarm.getSeverity(),
                alarm.getTimestamp(),
                alarm.getDescription());
    }
}
