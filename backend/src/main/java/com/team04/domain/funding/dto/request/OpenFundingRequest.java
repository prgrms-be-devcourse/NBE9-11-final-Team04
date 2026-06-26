package com.team04.domain.funding.dto.request;

import jakarta.validation.constraints.NotNull;

public record OpenFundingRequest(
        @NotNull Long ideaId
) {
}
