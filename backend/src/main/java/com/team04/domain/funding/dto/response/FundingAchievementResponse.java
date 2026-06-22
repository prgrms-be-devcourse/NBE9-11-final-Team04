package com.team04.domain.funding.dto.response;

public record FundingAchievementResponse(
        Long ideaId,
        Long goalAmount,
        Long currentAmount,
        double achievementRate,
        int sponsorCount
) {

    public static FundingAchievementResponse from(FundingDetailResponse detail) {
        return new FundingAchievementResponse(
                detail.ideaId(),
                detail.goalAmount(),
                detail.currentAmount(),
                detail.achievementRate(),
                detail.sponsorCount()
        );
    }
}
