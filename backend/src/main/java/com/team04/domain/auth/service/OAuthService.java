package com.team04.domain.auth.service;

import com.team04.domain.auth.dto.request.OAuthLoginRequest;
import com.team04.domain.auth.dto.request.OAuthRegisterRequest;
import com.team04.domain.auth.dto.response.OAuthAuthorizeResponse;
import com.team04.domain.auth.dto.response.OAuthResponse;
import com.team04.domain.auth.dto.response.TokenResponse;
import com.team04.domain.auth.entity.SocialAccount;
import com.team04.domain.auth.oauth.OAuthProperties;
import com.team04.domain.auth.oauth.OAuthState;
import com.team04.domain.auth.oauth.client.GoogleOAuthClient;
import com.team04.domain.auth.oauth.client.KakaoOAuthClient;
import com.team04.domain.auth.oauth.dto.GoogleUserInfo;
import com.team04.domain.auth.oauth.dto.KakaoUserInfo;
import com.team04.domain.auth.provider.Provider;
import com.team04.domain.auth.repository.SocialAccountRepository;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.util.JwtUtil;
import com.team04.infra.redis.OAuthCsrfRepository;
import com.team04.infra.redis.OAuthStateRepository;
import com.team04.infra.redis.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final OAuthStateRepository oAuthStateRepository;
    private final OAuthCsrfRepository oAuthCsrfRepository;
    private final OAuthProperties oAuthProperties;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    public OAuthAuthorizeResponse getGoogleAuthorizeUrl(String redirectUri) {
        String state = oAuthCsrfRepository.generate();
        String url = UriComponentsBuilder.fromUriString(oAuthProperties.getGoogle().getAuthorizeUri())
                .queryParam("client_id", oAuthProperties.getGoogle().getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build().toUriString();
        return new OAuthAuthorizeResponse(url);
    }

    public OAuthAuthorizeResponse getKakaoAuthorizeUrl(String redirectUri) {
        String state = oAuthCsrfRepository.generate();
        String url = UriComponentsBuilder.fromUriString(oAuthProperties.getKakao().getAuthorizeUri())
                .queryParam("client_id", oAuthProperties.getKakao().getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build().toUriString();
        return new OAuthAuthorizeResponse(url);
    }

    @Transactional
    public OAuthResponse processGoogle(OAuthLoginRequest request) {
//        if (!oAuthCsrfRepository.validateAndDelete(request.state())) {
//            throw new CustomException(ErrorCode.INVALID_OAUTH_STATE);
//        }
        log.info("[OAuthService] processGoogle 진입. redirectUri={}", request.redirectUri());
        try {
            GoogleUserInfo userInfo = googleOAuthClient.getUserInfo(request.code(), request.redirectUri());
            log.info("[OAuthService] Google userInfo 획득 성공. email={}", userInfo.email());
            return processOAuth(Provider.GOOGLE, userInfo.id(), userInfo.email(), userInfo.name());
        } catch (CustomException e) {
            log.error("[OAuthService] Google OAuth 실패. errorCode={}", e.getErrorCode());
            throw e;
        } catch (Exception e) {
            log.error("[OAuthService] Google OAuth 예상치 못한 예외. type={}, message={}", e.getClass().getName(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public OAuthResponse processKakao(OAuthLoginRequest request) {
        if (!oAuthCsrfRepository.validateAndDelete(request.state())) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_STATE);
        }

        KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(request.code(), request.redirectUri());

        if (userInfo.kakaoAccount() == null || userInfo.kakaoAccount().email() == null) {
            throw new CustomException(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }

        String providerId = String.valueOf(userInfo.id());
        String email = userInfo.kakaoAccount().email();
        String name = userInfo.kakaoAccount().profile() != null
                ? userInfo.kakaoAccount().profile().nickname()
                : email.split("@")[0];

        return processOAuth(Provider.KAKAO, providerId, email, name);
    }

    @Transactional
    public TokenResponse register(OAuthRegisterRequest request) {
        OAuthState state = oAuthStateRepository.findAndDelete(request.oauthToken())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_OAUTH_TOKEN));

        if (userRepository.existsByEmail(state.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = userRepository.save(User.create(
                state.email(),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                request.name(),
                request.nickname(),
                request.age(),
                com.team04.domain.user.entity.Role.USER
        ));

        socialAccountRepository.save(SocialAccount.create(state.provider(), state.providerId(), user));

        return issueTokens(user);
    }

    private OAuthResponse processOAuth(Provider provider, String providerId, String email, String name) {
        return socialAccountRepository.findByProviderAndProviderId(provider, providerId)
                .map(socialAccount -> {
                    User user = socialAccount.getUser();
                    validateUserStatus(user);
                    TokenResponse tokens = issueTokens(user);
                    return OAuthResponse.ofLogin(tokens.accessToken(), tokens.refreshToken());
                })
                .orElseGet(() -> {
                    String oauthToken = oAuthStateRepository.save(new OAuthState(provider, providerId, email, name));
                    return OAuthResponse.ofRegister(oauthToken, email, name);
                });
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, REFRESH_TOKEN_TTL);
        return new TokenResponse(accessToken, refreshToken);
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.ACCOUNT_WITHDRAWN);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
    }
}
