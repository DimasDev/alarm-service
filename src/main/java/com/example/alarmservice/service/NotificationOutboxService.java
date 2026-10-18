package com.example.alarmservice.service;

import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.entity.NotificationOutbox;
import com.example.alarmservice.model.NotificationStatus;
import com.example.alarmservice.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static com.example.alarmservice.exception.ErrorMessages.NOTIFICATION_SERIALIZATION_FAILED;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void saveNotification(AlarmEventRequest request) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
        } catch (JacksonException exception) {
            throw new IllegalStateException(NOTIFICATION_SERIALIZATION_FAILED, exception);
        }

        NotificationOutbox notification = new NotificationOutbox();

        notification.setEventId(request.eventId());
        notification.setElementId(request.elementId());
        notification.setPayload(payload);
        notification.setStatus(NotificationStatus.PENDING);

        outboxRepository.save(notification);
    }
}
