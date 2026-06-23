package com.team04.domain.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 토스 웹훅 페이로드 — DEPOSIT_CALLBACK(평면) / PAYMENT_STATUS_CHANGED(중첩) / 테스트용 단순 스키마 지원.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossWebhookRequest(
        String eventType,
        String eventId,
        String createdAt,
        String secret,
        String orderId,
        String status,
        Long amount,
        String transactionKey,
        String paymentKey,
        TossWebhookData data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossWebhookData(
            String paymentKey,
            String orderId,
            String status,
            Long totalAmount,
            String transactionKey,
            String secret
    ) {
    }

    public String resolvedEventType() {
        if (eventType != null && !eventType.isBlank()) {
            return eventType;
        }
        if (secret != null && transactionKey != null) {
            return "DEPOSIT_CALLBACK";
        }
        return "LEGACY";
    }

    public String resolvedOrderId() {
        if (orderId != null && !orderId.isBlank()) {
            return orderId;
        }
        return data != null ? data.orderId() : null;
    }

    public String resolvedStatus() {
        if (status != null && !status.isBlank()) {
            return status;
        }
        return data != null ? data.status() : null;
    }

    public Long resolvedAmount() {
        if (amount != null) {
            return amount;
        }
        return data != null ? data.totalAmount() : null;
    }

    public String resolvedSecret() {
        if (secret != null && !secret.isBlank()) {
            return secret;
        }
        return data != null ? data.secret() : null;
    }

    public String resolvedTransactionKey() {
        if (transactionKey != null && !transactionKey.isBlank()) {
            return transactionKey;
        }
        return data != null ? data.transactionKey() : null;
    }

    public String resolvedPaymentKey() {
        if (paymentKey != null && !paymentKey.isBlank()) {
            return paymentKey;
        }
        return data != null ? data.paymentKey() : null;
    }
}
