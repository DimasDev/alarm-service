package com.example.alarmservice.exception;

public class UnknownAlarmException extends RuntimeException {

    public UnknownAlarmException(String elementId, String alarmCode) {
        super("No active alarm found for elementId: " + elementId + " and alarmCode: " + alarmCode);
    }
}
