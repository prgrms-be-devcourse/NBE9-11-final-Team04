package com.team04.global.event;

import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.user.entity.Role;

public record ReportNotificationEvent(
        Role notifyRole,                // null이면 전체 사용자
        NotificationType notificationType,
        String title,
        String message,
        Long referenceId
) {
    public ReportNotificationEvent {
        java.util.Objects.requireNonNull(notificationType, "notificationType must not be null");
        java.util.Objects.requireNonNull(title, "title must not be null");
        java.util.Objects.requireNonNull(message, "message must not be null");
    }
}
