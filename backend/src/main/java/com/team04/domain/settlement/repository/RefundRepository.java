package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByStatus(RefundStatus status);

    /** 후원자별 환불 내역 조회 */
    List<Refund> findBySponsorId(Long sponsorId);

    /** 결제 건별 환불 내역 조회 */
    List<Refund> findByPaymentId(Long paymentId);

    Set<Refund> findByPaymentIdIn(Collection<Long> paymentIds);

    boolean existsByPaymentId(Long paymentId);
}
