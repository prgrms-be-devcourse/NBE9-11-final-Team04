package com.team04.domain.payment.entity;

import com.team04.global.entity.BaseEntity;
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

    public void complete(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentTypes.PaymentStatus.SUCCESS;
        this.approvedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = PaymentTypes.PaymentStatus.FAILED;
    }
}
