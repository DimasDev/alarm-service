package com.example.alarmservice.repository;

import com.example.alarmservice.entity.AlarmHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmHistoryRepository extends JpaRepository<AlarmHistory, Long> {

    boolean existsByEventId(String eventId);
}
