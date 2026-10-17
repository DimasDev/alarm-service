package com.example.alarmservice.controller;

import com.example.alarmservice.dto.ActiveAlarmResponse;
import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.dto.AlarmHistoryResponse;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.service.AlarmQueryService;
import com.example.alarmservice.service.AlarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final AlarmQueryService alarmQueryService;

    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "timestamp",
            "severity",
            "status",
            "elementId",
            "alarmCode"
    );

    @PostMapping
    public ResponseEntity<Void> receiveAlarm(@Valid @RequestBody AlarmEventRequest request) {
        boolean created = alarmService.processAlarm(request);
        if (!created) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/active")
    public List<ActiveAlarmResponse> getActiveAlarms() {
        return alarmQueryService.getActiveAlarms();
    }

    @GetMapping("/elements/{elementId}")
    public List<ActiveAlarmResponse> getActiveAlarmsByElement(@PathVariable String elementId) {
        return alarmQueryService.getActiveAlarmsByElement(elementId);
    }

    @GetMapping("/statistics")
    public Map<AlarmSeverity, Long> getStatistics() {
        return alarmQueryService.getStatistics();
    }

    @GetMapping("/history")
    public Page<AlarmHistoryResponse> getHistory(@RequestParam(required = false) String elementId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(defaultValue = "id") String sortBy,
                                                 @RequestParam(defaultValue = "desc") String direction) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must not be less than zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field: " + sortBy);
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direction should be asc or desc");
        }

        return alarmQueryService.getHistory(elementId, page, size, sortDirection, sortBy);
    }
}
