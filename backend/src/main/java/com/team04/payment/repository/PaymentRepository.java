package com.team04.payment.repository;

import com.team04.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByFundingId(Long fundingId);

    Optional<Payment> findByOrderId(String orderId);
}
