package com.team04.domain.idea.dto.response;

import com.team04.domain.idea.entity.Idea;

import java.time.LocalDateTime;

/** 프로젝트 목록과 검색 결과에 사용하는 아이디어 요약 응답 DTO입니다. */
public record IdeaSummaryResponse(
        Long ideaId,
        Long userId,
        String title,
        String category,
        String oneLineIntro,
        Long goalAmount,
        Long currentAmount,
        Long supporterCount,
        LocalDateTime fundingStartAt,
        LocalDateTime fundingEndAt,
        String status,
        LocalDateTime createdAt
) {

    /** 아이디어 엔티티를 목록용 요약 응답으로 변환합니다. */
    public static IdeaSummaryResponse of(Idea idea) {
        return new IdeaSummaryResponse(
                idea.getId(),
                idea.getUserId(),
                idea.getTitle(),
                idea.getCategory().name(),
                idea.getOneLineIntro(),
                idea.getGoalAmount(),
                idea.getCurrentAmount(),
                idea.getSupporterCount(),
                idea.getFundingStartAt(),
                idea.getFundingEndAt(),
                idea.getStatus().name(),
                idea.getCreatedAt()
        );
    }
}