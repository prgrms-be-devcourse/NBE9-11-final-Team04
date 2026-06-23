package com.team04.domain.payment.client.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public final class TossApiDtos {

    private TossApiDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossPaymentResponse(
            String paymentKey,
            String orderId,
            String status,
            Long totalAmount,
            String transactionKey,
            String secret,
            TossVirtualAccount virtualAccount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossVirtualAccount(
            String accountType,
            String accountNumber,
            String bankCode,
            String customerName,
            String dueDate
    ) {
        public LocalDateTime parseDueDate() {
            if (dueDate == null || dueDate.isBlank()) {
                return LocalDateTime.now().plusDays(3);
            }
            try {
                return OffsetDateTime.parse(dueDate).toLocalDateTime();
            } catch (Exception ignored) {
                return LocalDateTime.parse(dueDate.replace("Z", ""));
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossErrorResponse(
            String code,
            String message
    ) {
    }
}
