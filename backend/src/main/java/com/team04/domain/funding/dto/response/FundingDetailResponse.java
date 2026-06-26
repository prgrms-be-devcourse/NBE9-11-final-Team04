package com.team04.domain.funding.dto.response;

import com.team04.domain.idea.entity.Idea;

import java.time.LocalDateTime;

public record FundingDetailResponse(
        Long fundingId,
        Long ideaId,
        Long goalAmount,
        Long currentAmount,
        double achievementRate,
        int sponsorCount,
        LocalDateTime fundingStartAt,
        LocalDateTime fundingEndAt,
        String status
) {

    public static FundingDetailResponse from(Idea idea) {
        double rate = idea.getGoalAmount() > 0
                ? (double) idea.getCurrentAmount() / idea.getGoalAmount() * 100.0
                : 0.0;
        return new FundingDetailResponse(
                idea.getId(),
                idea.getId(),
                idea.getGoalAmount(),
                idea.getCurrentAmount(),
                Math.min(rate, 100.0),
                idea.getSponsorCount(),
                idea.getFundingStartAt(),
                idea.getFundingEndAt(),
                idea.getStatus().name()
        );
    }
}
