package com.team04.domain.payment.dto.response;

public record PaymentRefundResult(
        boolean success,
        String pgCancelKey,
        String failureMessage
) {

    public static PaymentRefundResult success(String pgCancelKey) {
        return new PaymentRefundResult(true, pgCancelKey, null);
    }

    public static PaymentRefundResult failure(String failureMessage) {
        return new PaymentRefundResult(false, null, failureMessage);
    }
}
