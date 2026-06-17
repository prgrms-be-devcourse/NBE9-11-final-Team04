package com.team04.domain.match.dto.response;

import com.team04.domain.match.entity.ExpertReview;
import com.team04.domain.match.entity.Feasibility;

import java.time.LocalDateTime;

public record ExpertReviewResponse(
        Long reviewId,
        Long matchId,
        Long ideaId,
        Long expertProfileId,
        Feasibility feasibility,
        String expectedPeriod,
        String techStack,
        String riskFactor,
        String opinion,
        LocalDateTime createdAt
) {
    public static ExpertReviewResponse from(ExpertReview review) {
        return new ExpertReviewResponse(
                review.getId(),
                review.getExpertMatch().getId(),
                review.getIdeaId(),
                review.getExpertProfile().getId(),
                review.getFeasibility(),
                review.getExpectedPeriod(),
                review.getTechStack(),
                review.getRiskFactor(),
                review.getOpinion(),
                review.getCreatedAt()
        );
    }
}
