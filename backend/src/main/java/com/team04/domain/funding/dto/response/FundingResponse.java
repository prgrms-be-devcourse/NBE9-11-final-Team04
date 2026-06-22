package com.team04.domain.funding.dto.response;

import java.time.LocalDateTime;

public record FundingResponse(
        Long fundingId,
        Long ideaId,
        Long sponsorId,
        Integer milestoneStep,
        Long amount,
        String rewardType,
        String status,
        LocalDateTime createdAt,
        LocalDateTime refundedAt
) {
}
