package com.team04.domain.auth.controller;

import com.team04.domain.auth.dto.request.*;
import com.team04.domain.auth.dto.response.TokenResponse;
import com.team04.domain.auth.service.AuthService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @RequestBody @Valid SignupRequest request,
            HttpServletResponse response
    ){
        TokenResponse tokenResponse = authService.signup(request);
        setTokenCookies(response, tokenResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ofSuccess(tokenResponse));
    }

    @PostMapping("/email-verify/send")
    public ApiResponse<Void> sendOtp(
            @RequestBody @Valid EmailSendRequest request
    ){
        authService.sendOtp(request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @PostMapping("/email-verify/confirm")
    public ApiResponse<Void> verifyOtp(
            @RequestBody @Valid EmailVerifyRequest request
    ){
        authService.verifyOtp(request);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ){
        TokenResponse tokenResponse = authService.login(request);
        setTokenCookies(response, tokenResponse);
        return ApiResponse.ofSuccess(tokenResponse);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response
    ){
        authService.logout(userDetails.getUserId());
        clearTokenCookies(response);
        return ApiResponse.ofSuccessWithoutBody();
    }

    @PostMapping("/token-refresh")
    public ApiResponse<TokenResponse> tokenRefresh(
            @RequestBody @Valid TokenRefreshRequest request,
            HttpServletResponse response
    ){
        TokenResponse tokenResponse = authService.tokenRefresh(request);
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
