package com.example.alarmservice.repository;

import com.example.alarmservice.entity.NotificationOutbox;
import com.example.alarmservice.model.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    @Query("""
            SELECT n
            FROM NotificationOutbox n
            WHERE n.status = :status
            ORDER BY n.id ASC
            """)
    List<NotificationOutbox> findPending(
            @Param("status") NotificationStatus status,
            Pageable pageable
    );
}
