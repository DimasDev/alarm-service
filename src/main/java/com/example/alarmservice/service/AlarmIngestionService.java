package com.example.alarmservice.service;

import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.dto.AlarmIngestionResult;
import com.example.alarmservice.model.NotificationStatus;
import com.example.alarmservice.notification.NotificationDispatcher;
import com.example.alarmservice.notification.NotificationHttpClient;
import com.example.alarmservice.notification.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    public AlarmIngestionResult processAlarm(AlarmEventRequest request) {
        if (!properties.enabled()) {
            boolean created = alarmService.processAlarm(request);

            return new AlarmIngestionResult(created, created ? NotificationStatus.DISABLED : NotificationStatus.DUPLICATE);
        }
        return lockManager.execute(request.elementId(), () -> processWithNotification(request));
    }

    private AlarmIngestionResult processWithNotification(AlarmEventRequest request) {
        try (var reservation = dispatcher.tryReserve(request.elementId())) {

            String payload = serialize(request);

            boolean created = alarmService.processAlarm(request);
            if (!created) {
                return new AlarmIngestionResult(false, NotificationStatus.DUPLICATE);
            }

            if (reservation == null) {
                log.warn("Notification skipped: queue is full, eventId={}, elementId={}", request.eventId(), request.elementId());
                return new AlarmIngestionResult(true, NotificationStatus.QUEUE_FULL);
            }

            reservation.submit(() -> sendNotification(request.eventId(), payload));

            return new AlarmIngestionResult(true, NotificationStatus.QUEUED);
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
