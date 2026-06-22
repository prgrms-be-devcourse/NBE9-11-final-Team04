package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.PaymentTypes.VbankDepositStatus;
import com.team04.domain.payment.entity.VbankDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VbankDepositRepository extends JpaRepository<VbankDeposit, Long> {

    Optional<VbankDeposit> findByPaymentId(Long paymentId);

    List<VbankDeposit> findByDepositStatusAndDueDateBefore(VbankDepositStatus depositStatus, LocalDateTime dueDate);
}
