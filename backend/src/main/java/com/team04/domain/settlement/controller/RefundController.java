package com.team04.domain.settlement.controller;

import com.team04.domain.payment.service.PaymentService;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.service.RefundService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
public class RefundController {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final RefundService refundService;
    private final PaymentService paymentService;

    /**
     * 환불 완료 처리
     * 결제팀 콜백용 — PENDING → COMPLETED
     */
    @PatchMapping("/{refundId}/complete")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable Long refundId,
            @RequestHeader(value = WEBHOOK_SECRET_HEADER, required = false) String webhookSecret
    ) {
        paymentService.verifyWebhookSecretOnly(webhookSecret);
        return ResponseEntity.ok(ApiResponse.ofSuccess(refundService.completeRefund(refundId)));
    }

    /**
     * 환불 실패 처리
     * 결제팀 콜백용 — PENDING → FAILED
     */
    @PatchMapping("/{refundId}/fail")
    public ResponseEntity<ApiResponse<RefundResponse>> failRefund(
            @PathVariable Long refundId,
            @RequestHeader(value = WEBHOOK_SECRET_HEADER, required = false) String webhookSecret
    ) {
        paymentService.verifyWebhookSecretOnly(webhookSecret);
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
