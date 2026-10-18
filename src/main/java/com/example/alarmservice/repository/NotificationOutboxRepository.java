package com.example.alarmservice.repository;

import com.example.alarmservice.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
}
