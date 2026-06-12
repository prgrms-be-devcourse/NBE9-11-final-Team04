package com.team04.payment.dto.request;

import com.team04.payment.entity.PaymentTypes.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull Long fundingId,
        @NotNull @Min(1) Long amount,
        @NotNull PaymentMethod method
) {
}
