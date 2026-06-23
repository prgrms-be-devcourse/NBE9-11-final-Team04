package com.team04.domain.payment.dto.response;

public record PaymentConfirmResult(
        boolean success,
        String paymentKey,
        String failureMessage,
        boolean awaitingDeposit,
        String tossWebhookSecret,
        VirtualAccountIssueResult virtualAccount
) {

    public static PaymentConfirmResult success(String paymentKey) {
        return new PaymentConfirmResult(true, paymentKey, null, false, null, null);
    }

    public static PaymentConfirmResult awaitingDeposit(
            String paymentKey,
            String tossWebhookSecret,
            VirtualAccountIssueResult virtualAccount
    ) {
        return new PaymentConfirmResult(true, paymentKey, null, true, tossWebhookSecret, virtualAccount);
    }

    public static PaymentConfirmResult failure(String failureMessage) {
        return new PaymentConfirmResult(false, null, failureMessage, false, null, null);
    }
}
