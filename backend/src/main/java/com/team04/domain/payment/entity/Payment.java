package com.team04.domain.payment.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fundingId;

    @Column(unique = true)
    private String paymentKey;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentTypes.PaymentMethod method;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentTypes.PaymentStatus status;

    private LocalDateTime approvedAt;

    private LocalDateTime refundedAt;

    /** 가상계좌 confirm 응답 secret — DEPOSIT_CALLBACK 웹훅 검증용 */
    @Column(length = 128)
    private String tossWebhookSecret;

    public static Payment createPending(
            Long fundingId,
            String orderId,
            Long amount,
            PaymentTypes.PaymentMethod method
    ) {
        Payment payment = new Payment();
        payment.fundingId = fundingId;
        payment.orderId = orderId;
        payment.amount = amount;
        payment.method = method;
        payment.status = PaymentTypes.PaymentStatus.PENDING;
        return payment;
    }

    public void registerVirtualAccountPending(String paymentKey, String tossWebhookSecret) {
        if (this.method != PaymentTypes.PaymentMethod.VIRTUAL_ACCOUNT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        if (this.status != PaymentTypes.PaymentStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.paymentKey = paymentKey;
        this.tossWebhookSecret = tossWebhookSecret;
    }

    public void complete(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentTypes.PaymentStatus.SUCCESS;
        this.approvedAt = LocalDateTime.now();
    }

    public void completeIfPending() {
        if (this.status == PaymentTypes.PaymentStatus.SUCCESS) {
            return;
        }
        if (this.status != PaymentTypes.PaymentStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.status = PaymentTypes.PaymentStatus.SUCCESS;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentTypes.PaymentStatus.FAILED;
    }

    public void markAsRefunded() {
        if (this.status != PaymentTypes.PaymentStatus.SUCCESS) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.status = PaymentTypes.PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }
}
