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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payment", description = "결제 생성·승인·환불·조회 및 PG 웹훅 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final PaymentService paymentService;

    @Operation(
            summary = "내 결제 내역 조회",
            description = "로그인 사용자의 결제 내역을 생성일 기준 내림차순으로 페이지네이션 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public ApiResponse<Page<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(paymentService.getMyPayments(userDetails.getUserId(), pageable));
    }

    @Operation(
            summary = "결제 생성",
            description = "후원 결제를 생성합니다. USER 역할 본인만 호출할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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

    @Operation(
            summary = "결제 설정 조회",
            description = "PG 클라이언트 키 등 프론트엔드 결제 연동에 필요한 설정을 조회합니다. 인증이 필요하지 않습니다."
    )
    @GetMapping("/config")
    public ApiResponse<PaymentConfigResponse> getPaymentConfig() {
        return ApiResponse.ofSuccess(paymentService.getPaymentConfig());
    }

    @Operation(
            summary = "시연용 결제 즉시 승인",
            description = "테스트 결제창 없이 결제를 즉시 완료 처리합니다. 시연·로컬 개발용입니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{paymentId}/demo-confirm")
    public ApiResponse<PaymentResponse> demoConfirmPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(
                paymentService.demoConfirmPayment(paymentId, userDetails.getUserId()));
    }

    @Operation(
            summary = "결제 승인",
            description = "PG 결제 완료 후 서버에서 결제를 최종 승인합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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

    @Operation(
            summary = "결제 환불",
            description = "후원 결제를 환불합니다. USER 역할 본인만 호출할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    
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

    @Operation(
            summary = "결제 상세 조회",
            description = "결제 ID로 결제 상세 정보를 조회합니다. 본인 결제 또는 관리자만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(
                paymentService.getPayment(paymentId, userDetails.getUserId(), userDetails.getRole())
        );
    }

    @Operation(
            summary = "토스페이먼츠 웹훅 처리",
            description = "토스페이먼츠 가상계좌 입금 완료(DONE) 웹훅을 수신·처리합니다. X-Webhook-Secret 헤더로 검증합니다."
    )
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
