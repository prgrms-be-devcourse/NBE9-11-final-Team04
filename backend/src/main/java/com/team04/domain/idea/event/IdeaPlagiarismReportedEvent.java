package com.team04.domain.idea.event;

/** 분쟁 도메인에서 처리할 아이디어 도용 신고 접수 이벤트입니다. */
public record IdeaPlagiarismReportedEvent(
        Long ideaId,
        Long ownerUserId,
        Long reporterUserId,
        String reason
) {
}
