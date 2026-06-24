package com.team04.domain.funding.dto.request;

import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayDepositRequest(
        @NotNull @Min(1) Long amount,
        @NotNull PaymentMethod paymentMethod
) {
}
