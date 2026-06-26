package com.team04.domain.user.dto.request;

import com.team04.domain.user.status.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull UserStatus status
) {}
