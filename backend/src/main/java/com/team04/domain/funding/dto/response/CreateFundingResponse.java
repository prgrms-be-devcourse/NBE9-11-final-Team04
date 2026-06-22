package com.team04.domain.funding.dto.response;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.payment.dto.response.PaymentResponse;

import java.time.LocalDateTime;

public record CreateFundingResponse(
        Long fundingId,
        Long ideaId,
        Long sponsorId,
        Integer milestoneStep,
        Long amount,
        String rewardType,
        String fundingStatus,
        LocalDateTime createdAt,
        PaymentResponse payment
) {

    public static CreateFundingResponse from(Funding funding, PaymentResponse payment) {
        return new CreateFundingResponse(
                funding.getId(),
                funding.getIdeaId(),
                funding.getSponsorId(),
                funding.getMilestoneStep(),
                funding.getAmount(),
                funding.getRewardType().name(),
                funding.getStatus().name(),
                funding.getCreatedAt(),
                payment
        );
    }
}
