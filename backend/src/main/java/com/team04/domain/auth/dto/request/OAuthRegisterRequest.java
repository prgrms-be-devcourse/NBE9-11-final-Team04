package com.team04.domain.auth.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OAuthRegisterRequest(
        @NotBlank String oauthToken,
        @NotBlank String name,
        @NotBlank String nickname,
        @Min(19) @Max(150) int age
) {
}
