package com.team04.payment.repository;

import com.team04.payment.entity.VbankDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VbankDepositRepository extends JpaRepository<VbankDeposit, Long> {

    Optional<VbankDeposit> findByPaymentId(Long paymentId);
}
