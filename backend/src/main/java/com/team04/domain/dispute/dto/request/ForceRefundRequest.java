package com.team04.domain.dispute.dto.request;

import jakarta.validation.constraints.NotNull;

public record ForceRefundRequest(
        @NotNull Long paymentId
) {
}
