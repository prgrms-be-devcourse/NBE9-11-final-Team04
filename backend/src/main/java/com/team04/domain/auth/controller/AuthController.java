package com.team04.domain.auth.controller;

import com.team04.domain.auth.dto.request.*;
import com.team04.domain.auth.dto.response.OAuthAuthorizeResponse;
import com.team04.domain.auth.dto.response.OAuthResponse;
import com.team04.domain.auth.dto.response.TokenResponse;
import com.team04.domain.auth.service.AuthService;
import com.team04.domain.auth.service.OAuthService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "인증", description = "회원가입 / 로그인 / OAuth / 토큰 갱신 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;

    @Operation(summary = "회원가입", description = "이메일·비밀번호 기반 회원가입. 성공 시 accessToken·refreshToken을 쿠키로 발급합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @RequestBody @Valid SignupRequest request,
            HttpServletResponse response
    ){
        TokenResponse tokenResponse = authService.signup(request);
        setTokenCookies(response, tokenResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ofSuccess(tokenResponse));
    }

    @Operation(summary = "이메일 인증 코드 발송", description = "입력한 이메일로 6자리 OTP 인증 코드를 발송합니다.")
    @PostMapping("/email-verify/send")
    public ApiResponse<Void> sendOtp(
            @RequestBody @Valid EmailSendRequest request
    ){
        authService.sendOtp(request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(summary = "이메일 인증 코드 확인", description = "발송된 OTP 코드를 검증합니다.")
    @PostMapping("/email-verify/confirm")
    public ApiResponse<Void> verifyOtp(
            @RequestBody @Valid EmailVerifyRequest request
    ){
        authService.verifyOtp(request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(summary = "로그인", description = "이메일·비밀번호로 로그인합니다. 성공 시 accessToken·refreshToken을 쿠키로 발급합니다.")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ){
        TokenResponse tokenResponse = authService.login(request);
        setTokenCookies(response, tokenResponse);
        return ApiResponse.ofSuccess(tokenResponse);
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하고 인증 쿠키를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response
    ){
        authService.logout(userDetails.getUserId());
        clearTokenCookies(response);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(summary = "관리자 초대 이메일 발송", description = "ADMIN 권한 필요. 초대 토큰이 포함된 이메일을 발송합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/admin-invitations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> sendAdminInvite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AdminInviteRequest request
    ) {
        authService.sendAdminInvite(userDetails.getUserId(), request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(summary = "관리자 회원가입", description = "초대 토큰을 검증하여 관리자 계정을 생성합니다.")
    @PostMapping("/admin-signup")
    public ResponseEntity<ApiResponse<TokenResponse>> adminSignup(
            @RequestBody @Valid AdminSignupRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.adminSignup(request);
        setTokenCookies(response, tokenResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ofSuccess(tokenResponse));
    }

    @Operation(summary = "구글 OAuth 인가 URL 조회", description = "Google 소셜 로그인을 위한 인가 URL을 반환합니다.")
    @GetMapping("/oauth2/google/authorize")
    public ApiResponse<OAuthAuthorizeResponse> getGoogleAuthorizeUrl(
            @Parameter(description = "OAuth 콜백 후 리다이렉트할 프론트엔드 URI") @RequestParam String redirectUri
    ) {
        return ApiResponse.ofSuccess(oAuthService.getGoogleAuthorizeUrl(redirectUri));
    }

    @Operation(summary = "카카오 OAuth 인가 URL 조회", description = "Kakao 소셜 로그인을 위한 인가 URL을 반환합니다.")
    @GetMapping("/oauth2/kakao/authorize")
    public ApiResponse<OAuthAuthorizeResponse> getKakaoAuthorizeUrl(
            @Parameter(description = "OAuth 콜백 후 리다이렉트할 프론트엔드 URI") @RequestParam String redirectUri
    ) {
        return ApiResponse.ofSuccess(oAuthService.getKakaoAuthorizeUrl(redirectUri));
    }

    @Operation(summary = "구글 OAuth 로그인/연동", description = "Google 인가 코드로 로그인(LOGIN) 또는 추가정보 입력 필요(REGISTER) 여부를 반환합니다.")
    @PostMapping("/oauth2/google")
    public ApiResponse<OAuthResponse> processGoogle(
            @RequestBody @Valid OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        OAuthResponse oAuthResponse = oAuthService.processGoogle(request);
        if ("LOGIN".equals(oAuthResponse.type())) {
            setTokenCookies(response, new TokenResponse(oAuthResponse.accessToken(), oAuthResponse.refreshToken()));
        }
        return ApiResponse.ofSuccess(oAuthResponse);
    }

    @Operation(summary = "카카오 OAuth 로그인/연동", description = "Kakao 인가 코드로 로그인(LOGIN) 또는 추가정보 입력 필요(REGISTER) 여부를 반환합니다.")
    @PostMapping("/oauth2/kakao")
    public ApiResponse<OAuthResponse> processKakao(
            @RequestBody @Valid OAuthLoginRequest request,
            HttpServletResponse response
    ) {
        OAuthResponse oAuthResponse = oAuthService.processKakao(request);
        if ("LOGIN".equals(oAuthResponse.type())) {
            setTokenCookies(response, new TokenResponse(oAuthResponse.accessToken(), oAuthResponse.refreshToken()));
        }
        return ApiResponse.ofSuccess(oAuthResponse);
    }

    @Operation(summary = "OAuth 추가정보 등록", description = "소셜 로그인 후 닉네임·나이 등 추가정보를 등록하여 회원가입을 완료합니다.")
    @PostMapping("/oauth2/register")
    public ResponseEntity<ApiResponse<TokenResponse>> oauthRegister(
            @RequestBody @Valid OAuthRegisterRequest request,
            HttpServletResponse response
    ) {
        TokenResponse tokenResponse = oAuthService.register(request);
        setTokenCookies(response, tokenResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ofSuccess(tokenResponse));
    }

    @Operation(summary = "액세스 토큰 갱신", description = "쿠키의 refreshToken을 이용해 새 accessToken·refreshToken을 발급합니다.")
    @PostMapping("/token-refresh")
    public ApiResponse<TokenResponse> tokenRefresh(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        TokenResponse tokenResponse = authService.tokenRefresh(new TokenRefreshRequest(refreshToken));
        setTokenCookies(response, tokenResponse);
        return ApiResponse.ofSuccess(tokenResponse);
    }

    private void setTokenCookies(HttpServletResponse response, TokenResponse tokenResponse) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokenResponse.accessToken())
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMinutes(30))
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenResponse.refreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
