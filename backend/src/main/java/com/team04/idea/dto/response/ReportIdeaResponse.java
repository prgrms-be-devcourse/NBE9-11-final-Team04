package com.team04.idea.dto.response;

/** 아이디어 도용 신고 접수 결과를 반환하는 응답 DTO입니다. */
public record ReportIdeaResponse(
        Long ideaId,
        Long reporterUserId,
        String message
) {
}