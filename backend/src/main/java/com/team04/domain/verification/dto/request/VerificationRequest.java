package com.team04.domain.verification.dto.request;

import java.util.List;

/** 프로젝트 검증 요청 접수 API의 요청 본문입니다. */
public record VerificationRequest(
        Long ideaId,
        String title,
        String description,
        List<Long> milestoneIds
) {
}
