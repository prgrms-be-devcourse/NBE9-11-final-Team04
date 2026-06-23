package com.team04.domain.user.dto.request;

import com.team04.domain.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(
        @NotNull Role role
) {}
