package com.team04.domain.idea.dto.response;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.verification.entity.TrustScore;

import java.util.Map;

/** 아이디어 신뢰도 점수와 항목별 세부 점수를 반환하는 응답 DTO입니다. */
public record TrustScoreResponse(
        Integer trustScore,
        String badge,
        Map<String, Integer> breakdown
) {

    public static TrustScoreResponse of(Idea idea, TrustScore trustScore) {
        return new TrustScoreResponse(
                trustScore.getTotalScore(),
                idea.getBadge().name(),
                Map.of(
                        "aiVerification", trustScore.getAiVerificationScore(),
                        "milestoneSpecificity", trustScore.getMilestoneSpecificityScore(),
                        "expertMatching", trustScore.getExpertMatchingScore(),
                        "adminApproval", trustScore.getAdminApprovalScore(),
                        "proposerHistory", trustScore.getProposerHistoryScore()
                )
        );
    }

    public static TrustScoreResponse ofDefault(Idea idea) {
        return new TrustScoreResponse(
                idea.getTrustScore() != null ? idea.getTrustScore() : 0,
                idea.getBadge().name(),
                Map.of(
                        "aiVerification", 0,
                        "milestoneSpecificity", 0,
                        "expertMatching", 0,
                        "adminApproval", 0,
                        "proposerHistory", 0
                )
        );
    }
}
