package com.team04.domain.settlement.dto.request;

import jakarta.validation.constraints.NotNull;

public record RefundRequest(
        @NotNull Long paymentId,
        @NotNull Long sponsorId
) {}