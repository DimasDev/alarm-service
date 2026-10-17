package com.example.alarmservice.exception;

import static com.example.alarmservice.exception.ErrorMessages.ALARM_NOT_FOUND;

public class UnknownAlarmException extends RuntimeException {

    public UnknownAlarmException(String elementId, String alarmCode) {
        super(ALARM_NOT_FOUND.formatted(elementId, alarmCode));
    }
}
