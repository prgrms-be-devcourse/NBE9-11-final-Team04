package com.team04.domain.dispute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDisputeRequest(
        @NotNull Long ideaId,
        @NotBlank String reason,
        String evidenceUrl
) {

}