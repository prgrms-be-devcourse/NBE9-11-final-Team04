package com.team04.domain.payment.controller;

import com.team04.domain.payment.dto.request.ConfirmPaymentRequest;
import com.team04.domain.payment.dto.request.CreatePaymentRequest;
import com.team04.domain.payment.dto.request.TossWebhookRequest;
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
 * 결제 API 컨트롤러 — 결제 생성·승인·환불·조회, PG 웹훅을 제공합니다.
 * 후원 결제 API는 SPONSOR 본인만 접근할 수 있습니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final PaymentService paymentService;

    // 내 결제 내역 조회 (페이징)
    @GetMapping("/me")
    public ApiResponse<Page<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(paymentService.getMyPayments(userDetails.getUserId(), pageable));
    }

    // 후원(funding)에 대한 결제 세션 생성 — SPONSOR 본인만
    @PostMapping
    public ApiResponse<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails.getRole() != Role.SPONSOR) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(paymentService.createPayment(request, userDetails.getUserId()));
    }

    // 카드 결제 PG 승인(confirm) 처리 — 후원자 본인만
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

    // 환불 요청 (스폰서 본인)
    @PostMapping("/{paymentId}/refund")
    public ApiResponse<Void> refundPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails.getRole() != Role.SPONSOR) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        paymentService.refundPayment(paymentId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    // 결제 단건 조회 — 후원자 본인 또는 ADMIN
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(
                paymentService.getPayment(paymentId, userDetails.getUserId(), userDetails.getRole())
        );
    }

    // 토스 가상계좌 입금 완료 웹훅 (status=DONE, X-Webhook-Secret 헤더 필요)
    @PostMapping("/webhooks/toss")
    public ApiResponse<Void> handleTossWebhook(
            @RequestHeader(WEBHOOK_SECRET_HEADER) String webhookSecret,
            @Valid @RequestBody TossWebhookRequest request
    ) {
        if ("DONE".equalsIgnoreCase(request.status())) {
            paymentService.processDepositWebhook(
                    request.orderId(),
                    request.amount(),
                    webhookSecret,
                    request.eventId()
            );
        }
        return ApiResponse.ofSuccessWithoutBody();
    }
}
