package com.team04.domain.funding.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayDepositRequest(
        @NotNull @Min(1) Long amount
) {
}
