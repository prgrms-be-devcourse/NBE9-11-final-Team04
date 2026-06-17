package com.team04.domain.payment.dto.response;

public record PaymentSessionResult(
        String clientKey,
        String redirectUrl
) {
}
