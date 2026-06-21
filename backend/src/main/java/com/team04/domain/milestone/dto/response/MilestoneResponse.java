package com.team04.domain.milestone.dto.response;

import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 마일스톤 응답 DTO입니다. */
public record MilestoneResponse(
        Long id,
        Long ideaId,
        Integer step,
        String goal,
        String expectedResult,
        LocalDate expectedDate,
        MilestoneStatus status,
        LocalDateTime createdAt
) {
    public static MilestoneResponse from(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getIdeaId(),
                milestone.getStep(),
                milestone.getGoal(),
                milestone.getExpectedResult(),
                milestone.getExpectedDate(),
                milestone.getStatus(),
                milestone.getCreatedAt()
        );
    }
}