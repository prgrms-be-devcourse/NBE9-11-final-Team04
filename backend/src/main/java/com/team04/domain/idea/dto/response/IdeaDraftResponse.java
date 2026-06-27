package com.team04.domain.idea.dto.response;

import com.team04.domain.idea.dto.request.CreateMilestoneRequest;
import com.team04.domain.idea.entity.IdeaDraft;
import com.team04.global.util.IdeaDraftMilestoneConverter;
import com.team04.global.util.ImageUrlConverter;

import java.time.LocalDateTime;
import java.util.List;

/** 아이디어 임시저장 상세 정보를 반환하는 응답 DTO입니다. */
public record IdeaDraftResponse(
        Long draftId,
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
        Long depositAmount,
        LocalDateTime fundingStartAt,
        LocalDateTime fundingEndAt,
        String rewardType,
        String imageUrl,
        List<String> imageUrls,
        List<CreateMilestoneRequest> milestones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 임시저장 엔티티를 응답 DTO로 변환합니다. */
    public static IdeaDraftResponse of(IdeaDraft draft) {
        return new IdeaDraftResponse(
                draft.getId(),
                draft.getUserId(),
                draft.getTitle(),
                draft.getCategory() == null ? null : draft.getCategory().name(),
                draft.getOneLineIntro(),
                draft.getProblemDefinition(),
                draft.getSolution(),
                draft.getGoal(),
                draft.getTargetCustomer(),
                draft.getCompetitor(),
                draft.getTeamIntro(),
                draft.getGoalAmount(),
                draft.getDepositAmount(),
                draft.getFundingStartAt(),
                draft.getFundingEndAt(),
                draft.getRewardType() == null ? null : draft.getRewardType().name(),
                draft.getImageUrl(),
                ImageUrlConverter.parse(draft.getImageUrls()),
                IdeaDraftMilestoneConverter.parse(draft.getMilestones()),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}
