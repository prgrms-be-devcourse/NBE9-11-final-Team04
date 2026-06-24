package com.team04.domain.verification.controller;

import com.team04.domain.verification.dto.response.AdminVerificationResponse;
import com.team04.domain.verification.service.VerificationAdminService;
import com.team04.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 관리자 검증 검토 목록 조회, 승인, 반려 API를 제공하는 컨트롤러입니다. */
@RestController
@RequestMapping("/admin/verifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class VerificationAdminController {

    private final VerificationAdminService verificationAdminService;

    /** PENDING_ADMIN_REVIEW 상태의 검증 목록을 페이지로 조회합니다. */
    @GetMapping
    public ApiResponse<Page<AdminVerificationResponse>> getReviews(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(verificationAdminService.getReviews(pageable));
    }

    /** 관리자가 검증을 통과 처리합니다. */
    @PatchMapping("/{verificationId}/pass")
    public ApiResponse<Void> pass(@PathVariable Long verificationId) {
        verificationAdminService.pass(verificationId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 관리자가 검증을 반려 처리합니다. */
    @PatchMapping("/{verificationId}/reject")
    public ApiResponse<Void> reject(
            @PathVariable Long verificationId,
            @RequestParam String reason
    ) {
        verificationAdminService.reject(verificationId, reason);
        return ApiResponse.ofSuccessWithoutBody();
    }
}
