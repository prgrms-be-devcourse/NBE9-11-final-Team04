package com.team04.domain.idea.dto.response;

import com.team04.domain.milestone.entity.Milestone;

import java.time.LocalDate;

/** 아이디어 상세 조회 시 함께 반환할 마일스톤 정보 응답 DTO입니다. */
public record IdeaMilestoneResponse(
        Long milestoneId,
        Integer step,
        String goal,
        String expectedResult,
        LocalDate expectedDate,
        String status
) {
    public static IdeaMilestoneResponse of(Milestone milestone) {
        return new IdeaMilestoneResponse(
                milestone.getId(),
                milestone.getStep(),
                milestone.getGoal(),
                milestone.getExpectedResult(),
                milestone.getExpectedDate(),
                milestone.getStatus().name()
        );
    }
}
