package com.team04.domain.settlement.controller;

import com.team04.domain.settlement.dto.request.RefundRequest;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.service.RefundService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /**
     * 분쟁 환불 생성 (관리자 전용)
     * GOAL_NOT_MET, CANCELLED는 내부 자동 생성 — 이 API는 DISPUTE 케이스만 처리
     * 환불 금액은 실제 결제 금액으로 고정 (과다 환불 방지)
     */
    @PostMapping("/dispute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> createDisputeRefund(
            @Valid @RequestBody RefundRequest request
    ) {
        RefundResponse response = refundService.createDisputeRefund(
                request.paymentId(),
                request.sponsorId()
        );
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /**
     * 환불 완료 처리 (관리자 전용)
     * TODO: 결제팀과 콜백 방식 협의 후 인증 처리 변경 필요 (현재 ADMIN 임시)
     */
    @PatchMapping("/{refundId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable Long refundId
    ) {
        RefundResponse response = refundService.completeRefund(refundId);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /**
     * 내 환불 내역 조회 (후원자 본인)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getMyRefunds(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RefundResponse> responses = refundService.getRefundsBySponsor(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(responses));
    }
}