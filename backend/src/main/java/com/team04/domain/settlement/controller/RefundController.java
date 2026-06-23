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

    /** 분쟁 환불 — PENDING 생성 후 PG 환불 실행 */
    @PostMapping("/dispute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> createDisputeRefund(
            @Valid @RequestBody RefundRequest request
    ) {
        RefundResponse response = refundService.createDisputeRefund(request.paymentId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /** 환불 완료 수동 처리 (PG 콜백 대체) */
    @PatchMapping("/{refundId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable Long refundId
    ) {
        RefundResponse response = refundService.completeRefund(refundId);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /** 실패한 환불 재시도 */
    @PatchMapping("/{refundId}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RefundResponse>> retryRefund(
            @PathVariable Long refundId
    ) {
        RefundResponse response = refundService.retryRefund(refundId);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getMyRefunds(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RefundResponse> responses = refundService.getRefundsBySponsor(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(responses));
    }
}
