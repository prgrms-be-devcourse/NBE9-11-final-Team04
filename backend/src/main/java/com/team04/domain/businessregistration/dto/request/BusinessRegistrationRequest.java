package com.team04.domain.businessregistration.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BusinessRegistrationRequest(
        @NotBlank
        String businessNumber
) {
}
