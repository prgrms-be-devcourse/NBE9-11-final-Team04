package com.team04.domain.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 완료/소명 보고서 반려 요청 DTO입니다. */
public record RejectReportRequest(
        @NotBlank(message = "반려 사유는 필수입니다.")
        String reason
) {
}
