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
     * 환불 완료 처리
     * 결제팀 콜백용 — PENDING → COMPLETED
     */
    @PatchMapping("/{refundId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable Long refundId
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(refundService.completeRefund(refundId)));
    }

    /**
     * 환불 실패 처리
     * 결제팀 콜백용 — PENDING → FAILED
     */
    @PatchMapping("/{refundId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> failRefund(
            @PathVariable Long refundId
    ) {
        return ResponseEntity.ok(ApiResponse.ofSuccess(refundService.failRefund(refundId)));
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