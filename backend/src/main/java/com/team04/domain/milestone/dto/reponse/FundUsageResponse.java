package com.team04.domain.milestone.dto.response;

import com.team04.domain.milestone.entity.FundUsage;

import java.time.LocalDate;

/** 자금 사용 내역 응답 DTO입니다. */
public record FundUsageResponse(
        Long fundUsageId,
        Long ideaId,
        String itemName,
        Long amount,
        LocalDate usedAt
) {
    public static FundUsageResponse from(FundUsage fundUsage) {
        return new FundUsageResponse(
                fundUsage.getId(),
                fundUsage.getIdeaId(),
                fundUsage.getItemName(),
                fundUsage.getAmount(),
                fundUsage.getUsedAt()
        );
    }
}