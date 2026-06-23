package com.team04.domain.notification.entity;

import com.team04.domain.user.entity.Role;
import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "notification_outbox", indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at ASC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox extends BaseEntity {

    private static final int MAX_RETRY = 3;
    private static final long[] BACKOFF_SECONDS = {30, 300, 1800, 3600};

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private Role targetRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationOutboxStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority;

    @Column(nullable = false)
    private int retryCount;

    private LocalDateTime nextRetryAt;
    private LocalDateTime processedAt;

    public static NotificationOutbox forUser(Long userId, NotificationType type,
                                             String title, String message, Long referenceId,
                                             NotificationPriority priority) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.userId = userId;
        outbox.notificationType = type;
        outbox.title = title;
        outbox.message = message;
        outbox.referenceId = referenceId;
        outbox.status = NotificationOutboxStatus.PENDING;
        outbox.priority = priority;
        outbox.retryCount = 0;
        return outbox;
    }

    public static NotificationOutbox forRole(Role role, NotificationType type,
                                             String title, String message, Long referenceId,
                                             NotificationPriority priority) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.targetRole = role;
        outbox.notificationType = type;
        outbox.title = title;
        outbox.message = message;
        outbox.referenceId = referenceId;
        outbox.status = NotificationOutboxStatus.PENDING;
        outbox.priority = priority;
        outbox.retryCount = 0;
        return outbox;
    }

    public boolean isRoleBased() {
        return this.targetRole != null;
    }

    public void markSent() {
        this.status = NotificationOutboxStatus.SENT;
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetryOrFail() {
        this.retryCount++;
        if (this.priority == NotificationPriority.CRITICAL) {
            int idx = Math.min(this.retryCount - 1, BACKOFF_SECONDS.length - 1);
            this.nextRetryAt = LocalDateTime.now().plusSeconds(BACKOFF_SECONDS[idx]);
        } else {
            if (this.retryCount >= MAX_RETRY) {
                this.status = NotificationOutboxStatus.FAILED;
                this.processedAt = LocalDateTime.now();
            }
        }
    }
}
