package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findBySponsorId(Long sponsorId);

    List<Refund> findByPaymentId(Long paymentId);

    Optional<Refund> findFirstByPaymentId(Long paymentId);

    boolean existsByPaymentId(Long paymentId);

    List<Refund> findByIdeaId(Long ideaId);

    boolean existsByIdeaIdAndStatus(Long ideaId, RefundStatus status);
}
