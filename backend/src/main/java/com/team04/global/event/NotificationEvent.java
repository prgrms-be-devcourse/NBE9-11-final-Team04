package com.team04.global.event;

import com.team04.domain.notification.entity.NotificationType;

public record NotificationEvent(
        Long userId,                       // 알림 받을 유저 ID (발행자가 직접 전달)
        NotificationType notificationType,
        String title,
        String message,
        Long targetId                      // 연관 리소스 ID (ideaId, fundingId 등)
) {}
