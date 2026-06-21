package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    /** 후원자별 환불 내역 조회 */
    List<Refund> findBySponsorId(Long sponsorId);

    /** 결제 건별 환불 내역 조회 */
    List<Refund> findByPaymentId(Long paymentId);

    boolean existsByPaymentId(Long paymentId);
}