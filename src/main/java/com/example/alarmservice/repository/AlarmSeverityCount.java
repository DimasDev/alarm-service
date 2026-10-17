package com.example.alarmservice.repository;

import com.example.alarmservice.model.AlarmSeverity;

public interface AlarmSeverityCount {
    AlarmSeverity getSeverity();

    long getCount();
}
