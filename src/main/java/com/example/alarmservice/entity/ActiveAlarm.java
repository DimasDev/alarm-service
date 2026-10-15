package com.example.alarmservice.entity;

import com.example.alarmservice.model.AlarmSeverity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "active_alarms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_active_alarm",
                columnNames = {"element_id", "alarm_code"}
        )
)
@Getter
@Setter
public class ActiveAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "element_id", nullable = false)
    private String elementId;

    @Column(name = "alarm_code", nullable = false)
    private String alarmCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmSeverity severity;

    @Column(name = "event_timestamp", nullable = false)
    private Long timestamp;

    @Column(columnDefinition = "TEXT")
    private String description;

}
