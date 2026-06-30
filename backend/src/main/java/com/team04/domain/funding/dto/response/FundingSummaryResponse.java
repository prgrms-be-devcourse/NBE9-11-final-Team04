package com.team04.domain.funding.dto.response;

import com.team04.domain.idea.entity.Idea;

import java.time.LocalDateTime;

public record FundingSummaryResponse(
        Long fundingId,
        Long ideaId,
        String title,
        String oneLineIntro,
        Long goalAmount,
        Long currentAmount,
        int sponsorCount,
        double achievementRate,
        LocalDateTime fundingStartAt,
        LocalDateTime fundingEndAt,
        String status,
        String rewardType
) {

    public static FundingSummaryResponse from(Idea idea) {
        return from(idea, idea.getSponsorCount());
    }

    public static FundingSummaryResponse from(Idea idea, int sponsorCount) {
        double rate = idea.getGoalAmount() > 0
                ? idea.getCurrentAmount() * 100.0 / idea.getGoalAmount()
                : 0.0;

        return new FundingSummaryResponse(
                idea.getId(),
                idea.getId(),
                idea.getTitle(),
                idea.getOneLineIntro(),
                idea.getGoalAmount(),
                idea.getCurrentAmount(),
                sponsorCount,
                Math.min(rate, 100.0),
                idea.getFundingStartAt(),
                idea.getFundingEndAt(),
                idea.getStatus().name(),
                idea.getRewardType().name()
        );
    }
}
