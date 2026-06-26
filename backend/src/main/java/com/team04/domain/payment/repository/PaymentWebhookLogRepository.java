package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.PaymentWebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {

    boolean existsByEventId(String eventId);

    Optional<PaymentWebhookLog> findByEventId(String eventId);
}
