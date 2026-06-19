package com.team04.global.event;

public record ReportNotificationEvent(
        Long targetId,
        ReportTargetType targetType,
        Long reporterUserId,
        String reason
) {
    public ReportNotificationEvent {
        java.util.Objects.requireNonNull(targetId, "targetId must not be null");
        java.util.Objects.requireNonNull(targetType, "targetType must not be null");
        java.util.Objects.requireNonNull(reporterUserId, "reporterUserId must not be null");
        java.util.Objects.requireNonNull(reason, "reason must not be null");
    }
}