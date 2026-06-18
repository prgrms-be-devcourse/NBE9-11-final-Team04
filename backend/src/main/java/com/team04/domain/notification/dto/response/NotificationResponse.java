package com.team04.domain.notification.dto.response;

import com.team04.domain.notification.entity.Notification;
import com.team04.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long referenceId,
        boolean isRead,
        LocalDateTime createdAt
) {
    public NotificationResponse(Notification notification){
        this(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
                );
    }
}
