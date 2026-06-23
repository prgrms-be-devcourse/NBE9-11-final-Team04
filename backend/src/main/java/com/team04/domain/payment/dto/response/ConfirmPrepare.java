package com.team04.domain.payment.dto.response;

import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;

public record ConfirmPrepare(
        Long paymentId,
        String orderId,
        Long amount,
        PaymentMethod method
) {

    public static ConfirmPrepare from(Payment payment) {
        return new ConfirmPrepare(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getMethod()
        );
    }
}
