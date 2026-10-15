package com.example.alarmservice.service;

import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.entity.ActiveAlarm;
import com.example.alarmservice.entity.AlarmHistory;
import com.example.alarmservice.exception.UnknownAlarmException;
import com.example.alarmservice.model.AlarmStatus;
import com.example.alarmservice.repository.ActiveAlarmRepository;
import com.example.alarmservice.repository.AlarmHistoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmHistoryRepository alarmHistoryRepository;
    private final ActiveAlarmRepository activeAlarmRepository;
    private final EntityManager entityManager;

    @Transactional
    public boolean processAlarm(AlarmEventRequest request) {
        acquireLock(1, request.eventId());
        acquireLock(2, request.elementId() + ":" + request.alarmCode());

        if (alarmHistoryRepository.existsByEventId(request.eventId())) {
            return false;
        }

        var active = activeAlarmRepository.findByElementIdAndAlarmCode(request.elementId(), request.alarmCode());

        if (request.status() == AlarmStatus.CLEARED && active.isEmpty()) {
            throw new UnknownAlarmException(request.elementId(), request.alarmCode());
        }

        AlarmHistory history = new AlarmHistory();
        history.setEventId(request.eventId());
        history.setElementId(request.elementId());
        history.setAlarmCode(request.alarmCode());
        history.setSeverity(request.severity());
        history.setStatus(request.status());
        history.setTimestamp(request.timestamp());
        history.setDescription(request.description());

        alarmHistoryRepository.save(history);

        if (request.status() == AlarmStatus.RAISED) {
            ActiveAlarm alarm = active.orElseGet(ActiveAlarm::new);

            alarm.setElementId(request.elementId());
            alarm.setAlarmCode(request.alarmCode());
            alarm.setSeverity(request.severity());
            alarm.setTimestamp(request.timestamp());
            alarm.setDescription(request.description());

            activeAlarmRepository.save(alarm);
        } else {
            activeAlarmRepository.delete(active.orElseThrow());
        }

        return true;
    }

    private void acquireLock(int namespace, String key) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:namespace, hashtext(:key))")
                .setParameter("namespace", namespace)
                .setParameter("key", key)
                .getSingleResult();
    }

}
