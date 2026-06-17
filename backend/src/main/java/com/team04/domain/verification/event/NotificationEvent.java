package com.team04.domain.verification.event;

/** 알림 도메인 미구현 상태에서 도메인 간 연동 계약만 표현하는 알림 이벤트입니다. */
public record NotificationEvent(
        Long ideaId,
        String title,
        String message
) {
}
