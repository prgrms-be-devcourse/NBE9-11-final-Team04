package com.team04.domain.auth.dto.request;

import jakarta.validation.constraints.*;

public record AdminSignupRequest(
        @NotBlank String inviteToken,
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$") String password,
        @NotBlank String name,
        @NotBlank String nickname,
        @Min(19) @Max(150) int age
) {
}
