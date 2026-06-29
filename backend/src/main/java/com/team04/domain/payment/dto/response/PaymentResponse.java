package com.team04.domain.payment.dto.response;

import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long fundingId,
        Long ideaId,
        String ideaTitle,
        String orderId,
        Long amount,
        PaymentStatus status,
        PaymentMethod method,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        String clientKey,
        String redirectUrl,
        VbankInfo vbank
) {

    public record VbankInfo(
            String bankCode,
            String accountNumber,
            LocalDateTime dueDate
    ) {
    }
}
