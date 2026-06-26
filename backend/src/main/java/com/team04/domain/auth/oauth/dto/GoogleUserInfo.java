package com.team04.domain.auth.oauth.dto;

public record GoogleUserInfo(
        String id,
        String email,
        String name
) {
}
