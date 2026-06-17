package com.team04.domain.payment.dto.response;

public record PaymentConfirmResult(
        boolean success,
        String paymentKey,
        String failureMessage
) {

    public static PaymentConfirmResult success(String paymentKey) {
        return new PaymentConfirmResult(true, paymentKey, null);
    }

    public static PaymentConfirmResult failure(String failureMessage) {
        return new PaymentConfirmResult(false, null, failureMessage);
    }
}
