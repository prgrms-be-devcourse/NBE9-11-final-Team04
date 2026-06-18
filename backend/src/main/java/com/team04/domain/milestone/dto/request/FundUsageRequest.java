package com.team04.domain.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** 자금 사용 내역 입력 요청 DTO입니다. */
public record FundUsageRequest(
        @NotBlank(message = "항목명은 필수입니다") String itemName,
        @NotNull(message = "금액은 필수입니다") @Positive(message = "금액은 0보다 커야 합니다") Long amount,
        @NotNull(message = "사용 날짜는 필수입니다") LocalDate usedAt
) {
}