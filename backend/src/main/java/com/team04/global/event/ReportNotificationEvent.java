package com.team04.global.event;

public record ReportNotificationEvent(
        Long targetId,
        String targetType,   // "IDEA", "FUNDING" 등
        Long reporterUserId,
        String reason
) {}