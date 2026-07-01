package com.team04.domain.verification.controller;

import com.team04.domain.user.entity.Role;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.dto.response.VerificationResponse;
import com.team04.domain.verification.service.VerificationService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 프로젝트 검증 접수와 결과 조회 API를 제공하는 컨트롤러입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/verifications")
@Tag(name = "검증 - 사용자", description = "프로젝트 검증 요청 및 아이디어 검증 결과 조회 API")
public class VerificationController {

    private final VerificationService verificationService;

    /** 검증 요청을 접수하고 즉시 검증 진행 중 응답을 반환합니다. */
    @Operation(
            summary = "프로젝트 검증 요청",
            description = "검증 요청을 접수하고 즉시 검증 진행 중 응답을 반환합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ApiResponse<VerificationResponse> requestVerification(
            @Valid @RequestBody VerificationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(verificationService.requestVerification(request, userDetails.getUserId()));
    }

    /** 아이디어 검증 결과를 조회합니다. 관리자 또는 본인 아이디어 제안자만 가능합니다. */
    @Operation(
            summary = "아이디어 검증 결과 조회",
            description = "아이디어 검증 결과를 조회합니다. 관리자 또는 본인 아이디어 제안자만 가능합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<VerificationResponse> getVerification(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Role role = userDetails.getRole();
        if (role == Role.EXPERT) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(verificationService.getVerificationByIdeaId(ideaId, userDetails.getUserId(), role));
    }
}