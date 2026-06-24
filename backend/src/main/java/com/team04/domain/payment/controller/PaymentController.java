package com.team04.domain.payment.controller;

import com.team04.domain.payment.dto.request.ConfirmPaymentRequest;
import com.team04.domain.payment.dto.request.CreatePaymentRequest;
import com.team04.domain.payment.dto.request.TossWebhookRequest;
import com.team04.domain.payment.dto.response.PaymentConfigResponse;
import com.team04.domain.payment.dto.response.PaymentResponse;
import com.team04.domain.payment.service.PaymentService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API — 결제 생성·승인·환불·조회, PG 웹훅.
 * 후원 결제 생성·환불은 USER 본인만 호출할 수 있습니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final PaymentService paymentService;

    @GetMapping("/me")
    public ApiResponse<Page<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(paymentService.getMyPayments(userDetails.getUserId(), pageable));
    }

    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails.getRole() != Role.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(paymentService.createPayment(request, userDetails.getUserId()));
    }

    @GetMapping("/config")
    public ApiResponse<PaymentConfigResponse> getPaymentConfig() {
        return ApiResponse.ofSuccess(paymentService.getPaymentConfig());
    }

    /** 시연용 — 테스트 결제창 없이 즉시 결제 완료 */
    @PostMapping("/{paymentId}/demo-confirm")
    public ApiResponse<PaymentResponse> demoConfirmPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(
                paymentService.demoConfirmPayment(paymentId, userDetails.getUserId()));
    }

    @PostMapping("/{paymentId}/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(
            @PathVariable Long paymentId,
            @Valid @RequestBody ConfirmPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(
                paymentService.confirmPayment(paymentId, request, userDetails.getUserId())
        );
    }

    @PostMapping("/{paymentId}/refund")
    public ApiResponse<Void> refundPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails.getRole() != Role.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        paymentService.refundPayment(paymentId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(
                paymentService.getPayment(paymentId, userDetails.getUserId(), userDetails.getRole())
        );
    }

    @PostMapping("/webhooks/toss")
    public ApiResponse<Void> handleTossWebhook(
            @RequestHeader(value = WEBHOOK_SECRET_HEADER, required = false) String webhookSecret,
            @RequestBody TossWebhookRequest request
    ) {
        if (!"DONE".equalsIgnoreCase(request.resolvedStatus())) {
            return ApiResponse.ofSuccessWithoutBody();
        }

        String orderId = request.resolvedOrderId();
        if (orderId == null || orderId.isBlank()) {
            return ApiResponse.ofSuccessWithoutBody();
        }

        paymentService.processDepositWebhook(
                orderId,
                request.resolvedAmount(),
                webhookSecret,
                request.resolvedEventId(),
                request.resolvedSecret()
        );
        return ApiResponse.ofSuccessWithoutBody();
    }
}
