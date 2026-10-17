package com.example.alarmservice.repository;

import com.example.alarmservice.entity.AlarmHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmHistoryRepository extends JpaRepository<AlarmHistory, Long> {

    boolean existsByEventId(String eventId);

    Page<AlarmHistory> findByElementId(String elementId, Pageable pageable);
}
