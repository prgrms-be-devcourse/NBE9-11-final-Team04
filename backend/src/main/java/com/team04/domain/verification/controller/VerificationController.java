package com.team04.domain.verification.controller;

import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.dto.response.VerificationResponse;
import com.team04.domain.verification.service.VerificationService;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 프로젝트 검증 접수와 재제출 API를 제공하는 컨트롤러입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/verifications")
public class VerificationController {

    private final VerificationService verificationService;

    /** 검증 요청을 접수하고 즉시 검증 진행 중 응답을 반환합니다. */
    @PostMapping
    public ApiResponse<VerificationResponse> requestVerification(
            @Valid @RequestBody VerificationRequest request
    ) {
        return ApiResponse.ofSuccess(verificationService.requestVerification(request));
    }

    /** 보완된 검증 요청을 재제출하고 즉시 검증 진행 중 응답을 반환합니다. */
    @PostMapping("/{verificationId}/resubmit")
    public ApiResponse<VerificationResponse> resubmit(
            @PathVariable Long verificationId,
            @Valid @RequestBody VerificationRequest request
    ) {
        return ApiResponse.ofSuccess(verificationService.resubmit(verificationId, request));
    }
}