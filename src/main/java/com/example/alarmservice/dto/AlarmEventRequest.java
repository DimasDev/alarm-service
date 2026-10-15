package com.example.alarmservice.dto;

import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlarmEventRequest(@NotBlank String eventId,
                                @NotBlank String elementId,
                                @NotBlank String alarmCode,
                                @NotNull AlarmSeverity severity,
                                @NotNull AlarmStatus status,
                                @NotNull Long timestamp,
                                String description) {
}
