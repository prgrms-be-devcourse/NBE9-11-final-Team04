package com.team04.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerifyRequest(
        @Email @NotBlank String email,
        @NotBlank String otp
) {
}
