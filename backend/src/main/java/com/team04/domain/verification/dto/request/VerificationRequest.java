package com.team04.domain.verification.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** 프로젝트 검증 요청 접수 API의 요청 본문입니다. */
public record VerificationRequest(
        @NotNull Long ideaId,
        @NotBlank String title,
        @NotBlank String description,
        @NotEmpty @Valid List<MilestoneInfo> milestones
) {

    /** 검증 요청에 포함되는 마일스톤의 상세 정보입니다. */
    public record MilestoneInfo(
            @NotBlank String goal,
            @NotBlank String expectedResult,
            @NotNull LocalDate expectedDate,
            Long lockedAmount
    ) {
    }
}
