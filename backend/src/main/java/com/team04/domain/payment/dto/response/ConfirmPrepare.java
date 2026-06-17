package com.team04.domain.payment.dto.response;

import com.team04.domain.payment.entity.Payment;

public record ConfirmPrepare(
        Long paymentId,
        String orderId,
        Long amount
) {

    public static ConfirmPrepare from(Payment payment) {
        return new ConfirmPrepare(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount()
        );
    }
}
