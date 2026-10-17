package com.example.alarmservice.exception;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.alarmservice.exception.ErrorMessages.GENERIC_VALIDATION_FAILURE;
import static com.example.alarmservice.exception.ErrorMessages.MALFORMED_JSON;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String DELIMITER = "; ";
    private static final String COLON = ": ";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + COLON + error.getDefaultMessage())
                .collect(Collectors.joining(DELIMITER));
        return ResponseEntity.badRequest().body(Map.of(ERROR, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMalformedJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of(ERROR, MALFORMED_JSON.formatted(exception.getMostSpecificCause().getMessage())));
    }

    @ExceptionHandler(UnknownAlarmException.class)
    public ResponseEntity<Map<String, String>> handleUnknownAlarm(UnknownAlarmException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(ERROR, exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of(STATUS, ex.getStatusCode().value(), MESSAGE, ex.getReason()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(
            HandlerMethodValidationException ex) {

        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(GENERIC_VALIDATION_FAILURE);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        STATUS, 400,
                        MESSAGE, message
                ));
    }
}
