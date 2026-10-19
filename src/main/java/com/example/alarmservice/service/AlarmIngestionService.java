package com.example.alarmservice.service;

import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.notification.NotificationDispatcher;
import com.example.alarmservice.notification.NotificationHttpClient;
import com.example.alarmservice.notification.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmIngestionService {

    private final AlarmService alarmService;
    private final ElementLockManager lockManager;
    private final NotificationDispatcher dispatcher;
    private final NotificationHttpClient httpClient;
    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;

    public boolean processAlarm(AlarmEventRequest request) {
        if (!properties.enabled()) {
            return alarmService.processAlarm(request);
        }
        return lockManager.execute(request.elementId(), () -> processWithNotification(request));
    }

    private boolean processWithNotification(AlarmEventRequest request) {
        try (var reservation = dispatcher.tryReserve(request.elementId())) {
            if (reservation == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Notification queue is full");
            }
            String payload = serialize(request);

            boolean created = alarmService.processAlarm(request);
            if (!created) {
                return false;
            }

            reservation.submit(() -> sendNotification(request.eventId(), payload));

            return true;
        }
    }

    private String serialize(AlarmEventRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize notification payload", exception);
        }
    }

    private void sendNotification(String eventId, String payload) {
        try {
            httpClient.send(eventId, payload, Duration.ofMillis(properties.readTimeoutMs()));
            log.info("Notification delivered, eventId={}", eventId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Notification interrupted, eventId={}", eventId);
        } catch (Exception exception) {
            log.error("Notification failed, eventId={}, reason={}", eventId, exception.toString());
        }
    }
}
