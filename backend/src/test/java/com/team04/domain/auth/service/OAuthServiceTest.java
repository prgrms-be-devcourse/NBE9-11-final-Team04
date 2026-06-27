package com.team04.domain.auth.service;

import com.team04.domain.auth.dto.request.OAuthLoginRequest;
import com.team04.domain.auth.dto.request.OAuthRegisterRequest;
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
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.util.JwtUtil;
import com.team04.infra.redis.OAuthCsrfRepository;
import com.team04.infra.redis.OAuthStateRepository;
import com.team04.infra.redis.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock private GoogleOAuthClient googleOAuthClient;
    @Mock private KakaoOAuthClient kakaoOAuthClient;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private OAuthStateRepository oAuthStateRepository;
    @Mock private OAuthCsrfRepository oAuthCsrfRepository;
    @Mock private OAuthProperties oAuthProperties;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuthService oAuthService;

    private User activeUser() {
        User user = User.create("test@test.com", "pw", "홍길동", "길동이", 25, Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    // ─────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("OAuth 회원가입 성공")
    void register_성공() {
        OAuthRegisterRequest request = new OAuthRegisterRequest("oauthToken", "홍길동", "길동이", 25);
        OAuthState state = new OAuthState(Provider.GOOGLE, "google-id", "test@test.com", "홍길동");
        User user = activeUser();

        given(oAuthStateRepository.findAndDelete("oauthToken")).willReturn(Optional.of(state));
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(userRepository.existsByNickname("길동이")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encodedPw");
        given(userRepository.save(any())).willReturn(user);
        given(jwtUtil.generateAccessToken(any(), any())).willReturn("accessToken");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refreshToken");

        TokenResponse response = oAuthService.register(request);

        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("OAuth 회원가입 실패 - 유효하지 않은 OAuth 토큰")
    void register_유효하지않은토큰_예외() {
        OAuthRegisterRequest request = new OAuthRegisterRequest("invalidToken", "홍길동", "길동이", 25);

        given(oAuthStateRepository.findAndDelete("invalidToken")).willReturn(Optional.empty());

        assertThatThrownBy(() -> oAuthService.register(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_OAUTH_TOKEN);
    }

    @Test
    @DisplayName("OAuth 회원가입 실패 - 이메일 중복")
    void register_이메일중복_예외() {
        OAuthRegisterRequest request = new OAuthRegisterRequest("oauthToken", "홍길동", "길동이", 25);
        OAuthState state = new OAuthState(Provider.GOOGLE, "google-id", "test@test.com", "홍길동");

        given(oAuthStateRepository.findAndDelete("oauthToken")).willReturn(Optional.of(state));
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        assertThatThrownBy(() -> oAuthService.register(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    @DisplayName("OAuth 회원가입 실패 - 닉네임 중복")
    void register_닉네임중복_예외() {
        OAuthRegisterRequest request = new OAuthRegisterRequest("oauthToken", "홍길동", "길동이", 25);
        OAuthState state = new OAuthState(Provider.GOOGLE, "google-id", "test@test.com", "홍길동");

        given(oAuthStateRepository.findAndDelete("oauthToken")).willReturn(Optional.of(state));
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(userRepository.existsByNickname("길동이")).willReturn(true);

        assertThatThrownBy(() -> oAuthService.register(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }

    // ─────────────────────────────────────────────
    // processGoogle
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("Google OAuth - 기존 소셜 계정이면 로그인 성공")
    void processGoogle_기존소셜계정_로그인성공() {
        User user = activeUser();
        SocialAccount socialAccount = SocialAccount.create(Provider.GOOGLE, "google-id", user);
        OAuthLoginRequest request = new OAuthLoginRequest("code", "http://redirect.uri", "state");
        GoogleUserInfo userInfo = new GoogleUserInfo("google-id", "test@test.com", "홍길동");

        given(googleOAuthClient.getUserInfo("code", "http://redirect.uri")).willReturn(userInfo);
        given(socialAccountRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-id"))
                .willReturn(Optional.of(socialAccount));
        given(jwtUtil.generateAccessToken(any(), any())).willReturn("accessToken");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refreshToken");

        OAuthResponse response = oAuthService.processGoogle(request);

        assertThat(response.type()).isEqualTo("LOGIN");
        assertThat(response.accessToken()).isEqualTo("accessToken");
    }

    @Test
    @DisplayName("Google OAuth - 미가입 유저면 REGISTER 응답 반환")
    void processGoogle_신규유저_REGISTER응답() {
        OAuthLoginRequest request = new OAuthLoginRequest("code", "http://redirect.uri", "state");
        GoogleUserInfo userInfo = new GoogleUserInfo("google-id", "test@test.com", "홍길동");

        given(googleOAuthClient.getUserInfo("code", "http://redirect.uri")).willReturn(userInfo);
        given(socialAccountRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-id"))
                .willReturn(Optional.empty());
        given(oAuthStateRepository.save(any())).willReturn("oauthToken");

        OAuthResponse response = oAuthService.processGoogle(request);

        assertThat(response.type()).isEqualTo("REGISTER");
        assertThat(response.oauthToken()).isEqualTo("oauthToken");
        assertThat(response.email()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("Google OAuth - 탈퇴한 유저는 예외 발생")
    void processGoogle_탈퇴한유저_예외() {
        User user = activeUser();
        user.withdraw();
        SocialAccount socialAccount = SocialAccount.create(Provider.GOOGLE, "google-id", user);
        OAuthLoginRequest request = new OAuthLoginRequest("code", "http://redirect.uri", "state");
        GoogleUserInfo userInfo = new GoogleUserInfo("google-id", "test@test.com", "홍길동");

        given(googleOAuthClient.getUserInfo("code", "http://redirect.uri")).willReturn(userInfo);
        given(socialAccountRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-id"))
                .willReturn(Optional.of(socialAccount));

        assertThatThrownBy(() -> oAuthService.processGoogle(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN);
    }

    @Test
    @DisplayName("Google OAuth - 정지된 유저는 예외 발생")
    void processGoogle_정지된유저_예외() {
        User user = activeUser();
        user.suspend();
        SocialAccount socialAccount = SocialAccount.create(Provider.GOOGLE, "google-id", user);
        OAuthLoginRequest request = new OAuthLoginRequest("code", "http://redirect.uri", "state");
        GoogleUserInfo userInfo = new GoogleUserInfo("google-id", "test@test.com", "홍길동");

        given(googleOAuthClient.getUserInfo("code", "http://redirect.uri")).willReturn(userInfo);
        given(socialAccountRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-id"))
                .willReturn(Optional.of(socialAccount));

        assertThatThrownBy(() -> oAuthService.processGoogle(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
    }

    // ─────────────────────────────────────────────
    // processKakao
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("Kakao OAuth - CSRF state 불일치 시 예외 발생")
    void processKakao_CSRF검증실패_예외() {
        OAuthLoginRequest request = new OAuthLoginRequest("code", "http://redirect.uri", "invalidState");

        given(oAuthCsrfRepository.validateAndDelete("invalidState")).willReturn(false);

        assertThatThrownBy(() -> oAuthService.processKakao(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_OAUTH_STATE);
    }

    @Test
    @DisplayName("Kakao OAuth - 이메일 미제공 시 예외 발생")
    void processKakao_이메일없음_예외() {
        OAuthLoginRequest request = new OAuthLoginRequest("code", "http://redirect.uri", "validState");
        KakaoUserInfo userInfo = new KakaoUserInfo(1L, null);

        given(oAuthCsrfRepository.validateAndDelete("validState")).willReturn(true);
        given(kakaoOAuthClient.getUserInfo("code", "http://redirect.uri")).willReturn(userInfo);

        assertThatThrownBy(() -> oAuthService.processKakao(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }

    @Test
    @DisplayName("Kakao OAuth - 기존 소셜 계정이면 로그인 성공")
    void processKakao_기존소셜계정_로그인성공() {
        User user = activeUser();
        SocialAccount socialAccount = SocialAccount.create(Provider.KAKAO, "1", user);
        OAuthLoginRequest request = new OAuthLoginRequest("code", "http://redirect.uri", "validState");
        KakaoUserInfo.KakaoAccount account = new KakaoUserInfo.KakaoAccount(
                "kakao@test.com",
                new KakaoUserInfo.KakaoProfile("카카오닉네임")
        );
        KakaoUserInfo userInfo = new KakaoUserInfo(1L, account);

        given(oAuthCsrfRepository.validateAndDelete("validState")).willReturn(true);
        given(kakaoOAuthClient.getUserInfo("code", "http://redirect.uri")).willReturn(userInfo);
        given(socialAccountRepository.findByProviderAndProviderId(Provider.KAKAO, "1"))
                .willReturn(Optional.of(socialAccount));
        given(jwtUtil.generateAccessToken(any(), any())).willReturn("accessToken");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refreshToken");

        OAuthResponse response = oAuthService.processKakao(request);

        assertThat(response.type()).isEqualTo("LOGIN");
    }
}
