package com.team04.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminInviteRequest(
        @Email @NotBlank String email
) {
}
