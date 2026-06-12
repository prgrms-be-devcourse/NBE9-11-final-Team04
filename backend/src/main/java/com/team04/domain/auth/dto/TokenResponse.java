package com.team04.domain.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
