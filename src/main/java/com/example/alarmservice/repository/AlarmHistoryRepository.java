package com.example.alarmservice.repository;

import com.example.alarmservice.entity.AlarmHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmHistoryRepository extends JpaRepository<AlarmHistory, Long> {

    boolean existsByEventId(String eventId);

    Page<AlarmHistory> findByElementId(String elementId, Pageable pageable);
}
