package com.team04.domain.auth.dto.response;

public record OAuthResponse(
        String type,
        String accessToken,
        String refreshToken,
        String oauthToken,
        String email,
        String name
) {
    public static OAuthResponse ofLogin(String accessToken, String refreshToken) {
        return new OAuthResponse("LOGIN", accessToken, refreshToken, null, null, null);
    }

    public static OAuthResponse ofRegister(String oauthToken, String email, String name) {
        return new OAuthResponse("REGISTER", null, null, oauthToken, email, name);
    }
}
