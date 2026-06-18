package com.team04.domain.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
