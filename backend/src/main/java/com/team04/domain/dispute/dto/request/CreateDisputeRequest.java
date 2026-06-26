package com.team04.domain.dispute.dto.request;

import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDisputeRequest(
        @NotNull TargetType targetType,
        @NotNull Long targetId,
        @NotNull Long reportedUserId,
        @NotNull DisputeCategory category,
        @NotBlank String title,
        @NotBlank String reason,
        String evidenceUrl
) {
}
