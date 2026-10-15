package com.example.alarmservice.repository;

import com.example.alarmservice.entity.ActiveAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActiveAlarmRepository extends JpaRepository<ActiveAlarm, Long> {

    Optional<ActiveAlarm> findByElementIdAndAlarmCode(String elementId, String alarmCode);
}
