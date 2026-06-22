package com.team04.global.event;

import com.team04.domain.notification.entity.NotificationPriority;
import com.team04.domain.notification.entity.NotificationType;

public record NotificationEvent(
        Long userId,
        NotificationType notificationType,
        String title,
        String message,
        Long targetId,
        NotificationPriority priority
) {
    public NotificationEvent(Long userId, NotificationType notificationType,
                              String title, String message, Long targetId) {
        this(userId, notificationType, title, message, targetId, NotificationPriority.NORMAL);
    }

    public NotificationEvent {
        java.util.Objects.requireNonNull(userId, "userId must not be null");
        java.util.Objects.requireNonNull(notificationType, "notificationType must not be null");
        java.util.Objects.requireNonNull(title, "title must not be null");
        java.util.Objects.requireNonNull(message, "message must not be null");
    }
}
