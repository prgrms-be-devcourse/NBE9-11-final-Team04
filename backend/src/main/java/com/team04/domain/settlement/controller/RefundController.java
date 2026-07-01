package com.team04.domain.settlement.controller;

import com.team04.domain.payment.service.PaymentService;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.service.RefundService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "환불", description = "환불 내역 조회 및 결제팀 환불 콜백 API")
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
    @Operation(
            summary = "환불 완료 처리",
            description = "결제팀 환불 완료 콜백입니다. X-Webhook-Secret 검증 후 환불 상태를 완료로 전환합니다."
    )
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
    @Operation(
            summary = "환불 실패 처리",
            description = "결제팀 환불 실패 콜백입니다. X-Webhook-Secret 검증 후 환불 상태를 실패로 전환합니다."
    )
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
    @Operation(
            summary = "내 환불 내역 조회",
            description = "로그인한 후원자 본인의 환불 내역을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getMyRefunds(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<RefundResponse> responses = refundService.getRefundsBySponsor(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(responses));
    }
}
