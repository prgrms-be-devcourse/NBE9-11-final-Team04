package com.team04.idea.event;

/** 알림 도메인에서 관리자 알림으로 처리할 아이디어 신고 이벤트입니다. */
public record IdeaReportNotificationEvent(
        Long ideaId,
        Long reporterUserId,
        String reason
) {
}
