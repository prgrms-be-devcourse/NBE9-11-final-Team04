package com.team04.domain.idea.dto.request;

import com.team04.domain.idea.entity.IdeaStatus;
import jakarta.validation.constraints.NotNull;

public record RestoreIdeaRequest(
        @NotNull(message = "복원할 상태는 필수입니다")
        IdeaStatus previousStatus
) {
}
