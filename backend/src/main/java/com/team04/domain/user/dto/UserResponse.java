package com.team04.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team04.domain.user.entity.Profile;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.common.Role;

import java.time.LocalDateTime;

public record UserResponse (
        Long id,
        String email,
        String name,
        String nickname,
        int age,
        Role role,
        UserStatus status,
        String intro,
        String portfolioUrl,
        String profileImage,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
        public UserResponse(User user, Profile profile) {
                this(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        user.getNickname(),
                        user.getAge(),
                        user.getRole(),
                        user.getStatus(),
                        profile != null ? profile.getIntro() : null,
                        profile != null ? profile.getPortfolioUrl() : null,
                        profile != null ? profile.getProfileImage() : null,
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                );
        }
}
