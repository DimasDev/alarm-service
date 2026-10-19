package com.example.alarmservice.dto;

import com.example.alarmservice.model.NotificationStatus;

public record AlarmIngestionResult(boolean created, NotificationStatus notificationStatus) {
}
