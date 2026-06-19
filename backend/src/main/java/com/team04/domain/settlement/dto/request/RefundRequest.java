package com.team04.domain.settlement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RefundRequest(
        @NotNull Long paymentId,
        @NotNull Long sponsorId,
        @Positive Long amount
) {}