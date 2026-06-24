package com.team04.domain.idea.dto.response;

import com.team04.domain.idea.entity.Idea;

import java.time.LocalDateTime;

/** 관리자 아이디어 심사 목록에 필요한 핵심 정보를 반환하는 응답 DTO입니다. */
public record AdminIdeaReviewResponse(
        Long ideaId,
        String title,
        String category,
        String status,
        String rejectReason,
        Integer trustScore,
        String badge,
        LocalDateTime createdAt
) {

    /** 아이디어 엔티티를 관리자 심사 응답 DTO로 변환합니다. */
    public static AdminIdeaReviewResponse of(Idea idea) {
        return new AdminIdeaReviewResponse(
                idea.getId(),
                idea.getTitle(),
                idea.getCategory().name(),
                idea.getStatus().name(),
                idea.getRejectReason(),
                idea.getTrustScore(),
                idea.getBadge().name(),
                idea.getCreatedAt()
        );
    }
}
