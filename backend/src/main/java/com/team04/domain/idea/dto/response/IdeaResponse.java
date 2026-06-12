package com.team04.domain.idea.dto.response;

import com.team04.domain.idea.entity.Idea;

import java.time.LocalDateTime;

/** 아이디어 전체 정보와 마일스톤 목록을 반환하는 응답 DTO입니다. */
public record IdeaResponse(
        Long ideaId,
        Long userId,
        String title,
        String category,
        String oneLineIntro,
        String problemDefinition,
        String solution,
        String goal,
        String targetCustomer,
        String competitor,
        String teamIntro,
        Long goalAmount,
        Long currentAmount,
        Long supporterCount,
        LocalDateTime fundingStartAt,
        LocalDateTime fundingEndAt,
        String rewardType,
        String status,
        Integer trustScore,
        String badge,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 아이디어 엔티티와 마일스톤 응답 목록을 조합해 응답 DTO를 생성합니다. */
    public static IdeaResponse of(Idea idea) {
        return new IdeaResponse(
                idea.getId(),
                idea.getUserId(),
                idea.getTitle(),
                idea.getCategory().name(),
                idea.getOneLineIntro(),
                idea.getProblemDefinition(),
                idea.getSolution(),
                idea.getGoal(),
                idea.getTargetCustomer(),
                idea.getCompetitor(),
                idea.getTeamIntro(),
                idea.getGoalAmount(),
                idea.getCurrentAmount(),
                idea.getSupporterCount(),
                idea.getFundingStartAt(),
                idea.getFundingEndAt(),
                idea.getRewardType().name(),
                idea.getStatus().name(),
                idea.getTrustScore(),
                idea.getBadge().name(),
                idea.getCreatedAt(),
                idea.getUpdatedAt()
        );
    }
}