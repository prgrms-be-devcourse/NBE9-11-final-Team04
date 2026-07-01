package com.team04.domain.verification.controller;

import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.dto.response.AdminVerificationResponse;
import com.team04.domain.verification.service.VerificationAdminService;
import com.team04.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/** 관리자 검증 장애 목록 조회와 수동 재시도 API를 제공하는 컨트롤러입니다. */
@RestController
@RequestMapping("/admin/verifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "검증 - 관리자", description = "관리자 검증 장애 목록 조회 및 수동 재시도 API")
public class VerificationAdminController {

    private final VerificationAdminService verificationAdminService;

    /** PENDING_ADMIN_REVIEW 상태의 검증 장애 목록을 페이지로 조회합니다. */
    @Operation(
            summary = "검증 장애 목록 조회",
            description = "PENDING_ADMIN_REVIEW 상태의 검증 장애 목록을 페이지로 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<Page<AdminVerificationResponse>> getFailures(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(verificationAdminService.getFailures(pageable));
    }

    /** 관리자가 검증 장애 건을 수동으로 재시도합니다. */
    @Operation(
            summary = "검증 장애 수동 재시도",
            description = "관리자가 검증 장애 건을 수동으로 재시도합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping ("/{verificationId}/retry")
    public ApiResponse<Void> retry(
            @PathVariable Long verificationId,
            @Valid @RequestBody VerificationRequest request
    ) {
        verificationAdminService.retry(verificationId, request);
        return ApiResponse.ofSuccessWithoutBody();
    }
}
