package com.team04.domain.auth.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfo(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {
    }

    public record KakaoProfile(
            String nickname
    ) {
    }
}
