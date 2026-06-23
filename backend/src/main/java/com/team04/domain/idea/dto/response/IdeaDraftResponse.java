package com.team04.domain.idea.dto.response;

import com.team04.domain.idea.entity.IdeaDraft;

import java.time.LocalDateTime;
import java.util.Arrays;
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
                parseImageUrls(draft.getImageUrls()),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }

    /** 콤마 구분 문자열로 저장된 임시저장 본문 이미지 URL 목록을 응답용 리스트로 변환합니다. */
    private static List<String> parseImageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.isBlank()) {
            return List.of();
        }

        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .toList();
    }
}
