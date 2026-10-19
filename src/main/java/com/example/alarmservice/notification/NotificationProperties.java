package com.example.alarmservice.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(boolean enabled, int workers, int queueCapacity, String endpoint,
                                     int connectTimeoutMs, int readTimeoutMs) {
}
