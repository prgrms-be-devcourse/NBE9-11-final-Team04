package com.team04.domain.milestone.dto.request;

import java.time.LocalDate;

/** 자금 사용 내역 입력 요청 DTO입니다. */
public record FundUsageRequest(String itemName, Long amount, LocalDate usedAt) {
}