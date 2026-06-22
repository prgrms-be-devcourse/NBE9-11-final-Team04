package com.team04.domain.notification.repository;

import com.team04.domain.notification.entity.NotificationOutbox;
import com.team04.domain.notification.entity.NotificationOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    @Query("""
            SELECT o FROM NotificationOutbox o
            WHERE o.status = :status
              AND (
                (o.priority = com.team04.domain.notification.entity.NotificationPriority.NORMAL
                    AND o.retryCount < :maxRetry)
                OR
                (o.priority = com.team04.domain.notification.entity.NotificationPriority.CRITICAL
                    AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now))
              )
            ORDER BY o.createdAt ASC
            """)
    List<NotificationOutbox> findProcessable(
            @Param("status") NotificationOutboxStatus status,
            @Param("maxRetry") int maxRetry,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
