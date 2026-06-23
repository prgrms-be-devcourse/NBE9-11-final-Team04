package com.team04.domain.notification.dto.request;

import com.team04.domain.user.entity.Role;
import jakarta.validation.constraints.NotBlank;

public record AnnouncementRequest(
        Role targetRole,    // null이면 전체 사용자
        @NotBlank String title,
        @NotBlank String message
) {}
