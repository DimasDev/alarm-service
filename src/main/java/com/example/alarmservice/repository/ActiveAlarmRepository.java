package com.example.alarmservice.repository;

import com.example.alarmservice.entity.ActiveAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ActiveAlarmRepository extends JpaRepository<ActiveAlarm, Long> {

    Optional<ActiveAlarm> findByElementIdAndAlarmCode(String elementId, String alarmCode);

    List<ActiveAlarm> findAllByElementId(String elementId);

    @Query("""
            SELECT a.severity AS severity,
                   COUNT(a) AS count
            FROM ActiveAlarm a
            GROUP BY a.severity
            """)
    List<AlarmSeverityCount> countActiveBySeverity();
}
