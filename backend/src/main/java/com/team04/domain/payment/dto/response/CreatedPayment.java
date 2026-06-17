package com.team04.domain.payment.dto.response;

import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;

import java.time.LocalDateTime;

public record CreatedPayment(
        Long id,
        Long fundingId,
        String orderId,
        Long amount,
        PaymentMethod method,
        LocalDateTime createdAt
) {

    public static CreatedPayment from(Payment payment) {
        return new CreatedPayment(
                payment.getId(),
                payment.getFundingId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getCreatedAt()
        );
    }
}
