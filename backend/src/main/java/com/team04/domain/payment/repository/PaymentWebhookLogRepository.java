package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.PaymentWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {

    boolean existsByEventId(String eventId);
}
