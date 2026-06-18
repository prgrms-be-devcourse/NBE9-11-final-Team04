package com.team04.domain.verification.dto.request;

import java.time.LocalDate;
import java.util.List;

/** 프로젝트 검증 요청 접수 API의 요청 본문입니다. */
public record VerificationRequest(
        Long ideaId,
        String title,
        String description,
        List<MilestoneInfo> milestones
) {

    /** 검증 요청에 포함되는 마일스톤의 상세 정보입니다. */
    public record MilestoneInfo(
            String goal,
            String expectedResult,
            LocalDate expectedDate,
            Long lockedAmount
    ) {
    }
}
