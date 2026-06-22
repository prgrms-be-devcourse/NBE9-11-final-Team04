package com.team04.domain.dispute.dto.request;

import com.team04.domain.dispute.entity.DisputeStatus;
import jakarta.validation.constraints.NotNull;

public record AdminDisputeStatusRequest(
        @NotNull DisputeStatus status
) {
}
