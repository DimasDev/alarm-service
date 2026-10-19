package com.example.alarmservice.controller;

import com.example.alarmservice.dto.ActiveAlarmResponse;
import com.example.alarmservice.dto.AlarmEventRequest;
import com.example.alarmservice.dto.AlarmHistoryResponse;
import com.example.alarmservice.dto.PageResponse;
import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.service.AlarmIngestionService;
import com.example.alarmservice.service.AlarmQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.alarmservice.exception.ErrorMessages.*;

@RestController
@RequestMapping("api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmIngestionService alarmIngestionService;
    private final AlarmQueryService alarmQueryService;

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
        boolean created = alarmIngestionService.processAlarm(request);
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
    public List<ActiveAlarmResponse> getActiveAlarmsByElement(@PathVariable @NotBlank String elementId) {
        return alarmQueryService.getActiveAlarmsByElement(elementId);
    }

    @GetMapping("/statistics")
    public Map<AlarmSeverity, Long> getStatistics() {
        return alarmQueryService.getStatistics();
    }

    @GetMapping("/history")
    public PageResponse<AlarmHistoryResponse> getHistory(@RequestParam(required = false) String elementId,
                                                         @RequestParam(defaultValue = "0")
                                                         @Min(value = 0, message = PAGE_NEGATIVE) int page,
                                                         @RequestParam(defaultValue = "20")
                                                         @Min(value = 1, message = SIZE_INVALID)
                                                         @Max(value = 100, message = SIZE_INVALID) int size,
                                                         @RequestParam(defaultValue = "id") String sortBy,
                                                         @RequestParam(defaultValue = "desc") String direction) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SORT_INVALID.formatted(sortBy));
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, DIRECTION_INVALID);
        }

        return PageResponse.from(alarmQueryService.getHistory(elementId, page, size, sortDirection, sortBy));
    }
}
