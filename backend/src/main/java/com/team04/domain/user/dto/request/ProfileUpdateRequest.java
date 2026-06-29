package com.team04.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        String intro,
        @Size(max = 500)
        String portfolioUrl
) {
}
