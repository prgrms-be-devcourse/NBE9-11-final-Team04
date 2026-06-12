package com.team04.payment.repository;

import com.team04.payment.entity.Payment;
import com.team04.payment.entity.PaymentTypes.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByFundingIdAndStatus(Long fundingId, PaymentStatus status);

    List<Payment> findByFundingIdOrderByCreatedAtDesc(Long fundingId);

    Optional<Payment> findByOrderId(String orderId);
}
