package com.team04.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank
        String nickname,
        String intro,
        @Size(max = 500)
        String portfolioUrl
) {
}
