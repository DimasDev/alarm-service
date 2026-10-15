package com.example.alarmservice.entity;

import com.example.alarmservice.model.AlarmSeverity;
import com.example.alarmservice.model.AlarmStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "alarm_history")
@Getter
@Setter
public class AlarmHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "element_id", nullable = false)
    private String elementId;

    @Column(name = "alarm_code", nullable = false)
    private String alarmCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlarmStatus status;

    @Column(name = "event_timestamp", nullable = false)
    private Long timestamp;

    @Column(columnDefinition = "TEXT")
    private String description;

}
