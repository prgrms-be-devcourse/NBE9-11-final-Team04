package com.team04.domain.settlement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자 강제 환불 요청 사유를 받는 DTO입니다. */
public record ForceRefundRequest(
        @NotBlank(message = "강제 환불 사유는 필수입니다")
        @Size(max = 500, message = "강제 환불 사유는 500자를 초과할 수 없습니다")
        String reason
) {
}
