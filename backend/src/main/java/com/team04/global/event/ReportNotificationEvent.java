package com.team04.global.event;

public record ReportNotificationEvent(
        Long targetId,
        ReportTargetType targetType,
        Long reporterUserId,
        String reason
) {}