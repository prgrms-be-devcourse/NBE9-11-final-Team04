package com.team04.domain.payment.client.dto;

public record PaymentSessionResult(
        String clientKey,
        String redirectUrl
) {
}
