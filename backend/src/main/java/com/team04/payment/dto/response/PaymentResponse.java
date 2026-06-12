package com.team04.payment.dto.response;

import com.team04.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.payment.entity.PaymentTypes.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long fundingId,
        String orderId,
        Long amount,
        PaymentStatus status,
        PaymentMethod method,
        LocalDateTime approvedAt,
        LocalDateTime createdAt
) {
}
