package com.team04.global.event;

import com.team04.domain.notification.entity.NotificationPriority;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.user.entity.Role;

public record ReportNotificationEvent(
        Role notifyRole,
        NotificationType notificationType,
        String title,
        String message,
        Long referenceId,
        NotificationPriority priority
) {
    public ReportNotificationEvent(Role notifyRole, NotificationType notificationType,
                                    String title, String message, Long referenceId) {
        this(notifyRole, notificationType, title, message, referenceId, NotificationPriority.NORMAL);
    }

    public ReportNotificationEvent {
        java.util.Objects.requireNonNull(notificationType, "notificationType must not be null");
        java.util.Objects.requireNonNull(title, "title must not be null");
        java.util.Objects.requireNonNull(message, "message must not be null");
    }
}
