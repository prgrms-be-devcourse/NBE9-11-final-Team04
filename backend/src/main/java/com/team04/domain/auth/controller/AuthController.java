package com.team04.domain.auth.controller;

import com.team04.domain.auth.dto.*;
import com.team04.domain.auth.service.AuthService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<TokenResponse>> signup(
            @RequestBody @Valid SignupRequest request
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ofSuccess(authService.signup(request)));
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
            @RequestBody @Valid LoginRequest request
    ){
        return ApiResponse.ofSuccess(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        authService.logout(userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    @PostMapping("/token-refresh")
    public ApiResponse<TokenResponse> tokenRefresh(
            @RequestBody @Valid TokenRefreshRequest request
    ){
        return ApiResponse.ofSuccess(authService.tokenRefresh(request));
    }
}
