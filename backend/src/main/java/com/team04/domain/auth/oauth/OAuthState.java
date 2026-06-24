package com.team04.domain.auth.oauth;

import com.team04.domain.auth.provider.Provider;

public record OAuthState(
        Provider provider,
        String providerId,
        String email,
        String name
) {
}
