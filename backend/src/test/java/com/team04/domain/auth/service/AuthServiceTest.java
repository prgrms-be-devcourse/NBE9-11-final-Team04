package com.team04.domain.auth.service;

import com.team04.domain.auth.dto.request.*;
import com.team04.domain.auth.dto.response.TokenResponse;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.common.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.util.JwtUtil;
import com.team04.infra.email.EmailService;
import com.team04.infra.redis.OtpRepository;
import com.team04.infra.redis.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User activeUser() {
        return User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);
    }

    // ==================== signup ====================

    @Test
    @DisplayName("회원가입 성공")
    void signup_성공() {
        SignupRequest request = new SignupRequest("test@test.com", "password1!", "홍길동", "길동이", 25, Role.PROPOSER);

        given(jwtUtil.generateAccessToken(any(), any())).willReturn("accessToken");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refreshToken");

        TokenResponse response = authService.signup(request);

        assertThat(response.accessToken()).isEqualTo("accessToken");
        then(refreshTokenRepository).should().save(any(), anyString(), any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_이메일중복() {
        SignupRequest request = new SignupRequest("test@test.com", "password1!", "홍길동", "길동이", 25, Role.PROPOSER);

        given(userRepository.existsByEmail("test@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        then(userRepository).should(never()).save(any());
    }

    // ==================== login ====================

    @Test
    @DisplayName("로그인 성공")
    void login_성공() {
        User user = activeUser();
        LoginRequest request = new LoginRequest("test@test.com", "password1!");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1!", "encodedPassword")).willReturn(true);
        given(jwtUtil.generateAccessToken(any(), any())).willReturn("accessToken");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refreshToken");

        TokenResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 실패 - 유저 없음")
    void login_유저없음() {
        LoginRequest request = new LoginRequest("test@test.com", "password1!");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_비밀번호불일치() {
        User user = activeUser();
        LoginRequest request = new LoginRequest("test@test.com", "wrongPassword");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("로그인 실패 - 탈퇴한 유저")
    void login_탈퇴한유저() {
        User user = activeUser();
        user.withdraw();
        LoginRequest request = new LoginRequest("test@test.com", "password1!");

        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN);
    }

    // ==================== logout ====================

    @Test
    @DisplayName("로그아웃 성공")
    void logout_성공() {
        authService.logout(1L);

        then(refreshTokenRepository).should().delete(1L);
    }

    // ==================== tokenRefresh ====================

    @Test
    @DisplayName("토큰 재발급 성공")
    void tokenRefresh_성공() {
        User user = activeUser();
        TokenRefreshRequest request = new TokenRefreshRequest("validRefreshToken");

        given(jwtUtil.validate("validRefreshToken")).willReturn(true);
        given(jwtUtil.getUserId("validRefreshToken")).willReturn(1L);
        given(refreshTokenRepository.find(1L)).willReturn(Optional.of("validRefreshToken"));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtUtil.generateAccessToken(any(), any())).willReturn("newAccessToken");
        given(jwtUtil.generateRefreshToken(any())).willReturn("newRefreshToken");

        TokenResponse response = authService.tokenRefresh(request);

        assertThat(response.accessToken()).isEqualTo("newAccessToken");
        assertThat(response.refreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰")
    void tokenRefresh_유효하지않은토큰() {
        TokenRefreshRequest request = new TokenRefreshRequest("invalidToken");

        given(jwtUtil.validate("invalidToken")).willReturn(false);

        assertThatThrownBy(() -> authService.tokenRefresh(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 로그아웃 상태 (Redis 없음)")
    void tokenRefresh_로그아웃상태() {
        TokenRefreshRequest request = new TokenRefreshRequest("validToken");

        given(jwtUtil.validate("validToken")).willReturn(true);
        given(jwtUtil.getUserId("validToken")).willReturn(1L);
        given(refreshTokenRepository.find(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.tokenRefresh(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 토큰 불일치")
    void tokenRefresh_토큰불일치() {
        TokenRefreshRequest request = new TokenRefreshRequest("tokenA");

        given(jwtUtil.validate("tokenA")).willReturn(true);
        given(jwtUtil.getUserId("tokenA")).willReturn(1L);
        given(refreshTokenRepository.find(1L)).willReturn(Optional.of("tokenB"));

        assertThatThrownBy(() -> authService.tokenRefresh(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 탈퇴한 유저")
    void tokenRefresh_탈퇴한유저() {
        User user = activeUser();
        user.withdraw();
        TokenRefreshRequest request = new TokenRefreshRequest("validToken");

        given(jwtUtil.validate("validToken")).willReturn(true);
        given(jwtUtil.getUserId("validToken")).willReturn(1L);
        given(refreshTokenRepository.find(1L)).willReturn(Optional.of("validToken"));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.tokenRefresh(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN);
    }

    // ==================== sendOtp ====================

    @Test
    @DisplayName("OTP 발송 성공")
    void sendOtp_성공() {
        EmailSendRequest request = new EmailSendRequest("test@test.com");

        authService.sendOtp(request);

        then(otpRepository).should().save(anyString(), anyString(), any());
        then(emailService).should().sendOtp(anyString(), anyString());
    }

    // ==================== verifyOtp ====================

    @Test
    @DisplayName("OTP 검증 성공")
    void verifyOtp_성공() {
        EmailVerifyRequest request = new EmailVerifyRequest("test@test.com", "123456");

        given(otpRepository.find("test@test.com")).willReturn(Optional.of("123456"));

        authService.verifyOtp(request);

        then(otpRepository).should().delete("test@test.com");
    }

    @Test
    @DisplayName("OTP 검증 실패 - 만료됨")
    void verifyOtp_만료됨() {
        EmailVerifyRequest request = new EmailVerifyRequest("test@test.com", "123456");

        given(otpRepository.find("test@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.OTP_EXPIRED);
    }

    @Test
    @DisplayName("OTP 검증 실패 - 코드 불일치")
    void verifyOtp_코드불일치() {
        EmailVerifyRequest request = new EmailVerifyRequest("test@test.com", "111111");

        given(otpRepository.find("test@test.com")).willReturn(Optional.of("999999"));

        assertThatThrownBy(() -> authService.verifyOtp(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_OTP);

        then(otpRepository).should(never()).delete(anyString());
    }
}
