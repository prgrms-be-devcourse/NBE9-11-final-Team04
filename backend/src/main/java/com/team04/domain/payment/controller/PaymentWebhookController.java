package com.team04.domain.payment.controller;

import com.team04.domain.payment.dto.request.TossWebhookRequest;
import com.team04.domain.payment.service.PaymentService;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PG 웹훅 수신 — 토스 가상계좌 입금 알림 등 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments/webhooks")
public class PaymentWebhookController {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final PaymentService paymentService;

    // 가상계좌 입금 완료 웹훅 (테스트: status=DONE, 헤더 X-Webhook-Secret 필요)
    @PostMapping("/toss")
    public ApiResponse<Void> handleTossWebhook(
            @RequestHeader(WEBHOOK_SECRET_HEADER) String webhookSecret,
            @Valid @RequestBody TossWebhookRequest request
    ) {
        if ("DONE".equalsIgnoreCase(request.status())) {
            paymentService.processDepositWebhook(request.orderId(), request.amount(), webhookSecret);
        }
        return ApiResponse.ofSuccessWithoutBody();
    }
}
