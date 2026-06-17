package com.team04.domain.payment.dto.response;

public record PaymentVerifyResult(
        boolean verified,
        String failureMessage
) {

    public static PaymentVerifyResult success() {
        return new PaymentVerifyResult(true, null);
    }

    public static PaymentVerifyResult failure(String failureMessage) {
        return new PaymentVerifyResult(false, failureMessage);
    }
}
