package com.example.alarmservice.exception;

public class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String PAGE_NEGATIVE = "Page index must not be less than zero";

    public static final String SIZE_INVALID = "Page size must be between 1 and 100";

    public static final String SORT_INVALID = "Invalid sort field: %s";

    public static final String DIRECTION_INVALID = "Direction should be asc or desc";

    public static final String ALARM_NOT_FOUND = "No active alarm found for elementId=%s and alarmCode=%s";

    public static final String MALFORMED_JSON = "Malformed JSON or unsupported field value: %s";

    public static final String GENERIC_VALIDATION_FAILURE = "Validation failure";

    public static final String NOTIFICATION_SERIALIZATION_FAILED = "Failed to serialize notification payload";
}
