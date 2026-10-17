package com.example.alarmservice.repository.projection;

import com.example.alarmservice.model.AlarmSeverity;

public interface AlarmSeverityCount {
    AlarmSeverity getSeverity();

    long getCount();
}
