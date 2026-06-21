package com.team04.domain.match.dto.request;

import com.team04.domain.match.entity.Feasibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExpertReviewRequest(
        @NotNull(message = "구현 가능 여부는 필수입니다")
        Feasibility feasibility,          // 구현 가능 POSSIBLE, 구현 불가 IMPOSSIBLE

        @NotBlank(message = "예상 기간은 필수입니다")
        String expectedPeriod,

        @NotBlank(message = "기술 스택은 필수입니다")
        String techStack,

        @NotBlank(message = "리스크 요인은 필수입니다")
        String riskFactor,

        @NotBlank(message = "검토 의견은 필수입니다")
        String opinion
) {
}
