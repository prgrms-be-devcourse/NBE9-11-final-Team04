package com.team04.domain.idea.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 임시저장용 마일스톤 요청 DTO — 모든 필드가 부분 입력을 허용합니다. */
public record DraftMilestoneRequest(
        @Min(1) @Max(3) Integer step,
        @Size(max = 1000) String goal,
        @Size(max = 1000) String expectedResult,
        LocalDate expectedDate
) {
}
