package com.team04.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 토스 가상계좌 입금 웹훅 요청 (테스트·연동용 최소 스키마) */
public record TossWebhookRequest(
        String eventId,
        @NotBlank String orderId,
        @NotBlank String status,
        @NotNull @Min(1) Long amount
) {
}
