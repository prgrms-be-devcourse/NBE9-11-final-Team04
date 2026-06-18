package com.team04.domain.settlement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 선정산 신청 요청 DTO입니다. */
public record PreSettlementRequest(
        @NotNull(message = "선정산 금액은 필수입니다")
        @Positive(message = "선정산 금액은 0보다 커야 합니다")
        Long amount
) {
}