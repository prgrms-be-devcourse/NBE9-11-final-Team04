package com.team04.domain.payment.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_webhook_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 30)
    private String provider;

    public static PaymentWebhookLog create(String eventId, String orderId, String status, Long amount, String provider) {
        PaymentWebhookLog log = new PaymentWebhookLog();
        log.eventId = eventId;
        log.orderId = orderId;
        log.status = status;
        log.amount = amount;
        log.provider = provider;
        return log;
    }
}
