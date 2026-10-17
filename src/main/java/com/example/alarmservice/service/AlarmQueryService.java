package com.example.alarmservice.service;

import com.example.alarmservice.dto.ActiveAlarmResponse;
import com.example.alarmservice.dto.AlarmHistoryResponse;
import com.example.alarmservice.entity.AlarmHistory;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.repository.ActiveAlarmRepository;
import com.example.alarmservice.repository.AlarmHistoryRepository;
import com.example.alarmservice.repository.AlarmSeverityCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmQueryService {

    private final ActiveAlarmRepository activeAlarmRepository;
    private final AlarmHistoryRepository alarmHistoryRepository;

    public List<ActiveAlarmResponse> getActiveAlarms() {
        return activeAlarmRepository.findAll().stream()
                .map(ActiveAlarmResponse::from)
                .toList();
    }

    public List<ActiveAlarmResponse> getActiveAlarmsByElement(String elementId) {
        return activeAlarmRepository.findAllByElementId(elementId).stream()
                .map(ActiveAlarmResponse::from)
                .toList();
    }

    public Map<AlarmSeverity, Long> getStatistics() {
        Map<AlarmSeverity, Long> statistics = new LinkedHashMap<>();
        for (AlarmSeverity severity : AlarmSeverity.values()) {
            statistics.put(severity, 0L);
        }
        for (AlarmSeverityCount result : activeAlarmRepository.countActiveBySeverity()) {
            statistics.put(result.getSeverity(), result.getCount());
        }
        return statistics;
    }

    public Page<AlarmHistoryResponse> getHistory(String elementId, int page, int size, Sort.Direction sortDirection, String sortBy) {
        Sort sort = Sort.by(sortDirection, sortBy);
        if (!sortBy.equals("id")) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AlarmHistory> history = elementId == null
                ? alarmHistoryRepository.findAll(pageable)
                : alarmHistoryRepository.findByElementId(elementId, pageable);
        return history.map(AlarmHistoryResponse::from);
    }
}
