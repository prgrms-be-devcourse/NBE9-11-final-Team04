package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByStatus(RefundStatus status);

    /** 후원자별 환불 내역 조회 */
    List<Refund> findBySponsorId(Long sponsorId);

    /** 결제 건별 환불 내역 조회 */
    List<Refund> findByPaymentId(Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Refund r WHERE r.id = :id")
    Optional<Refund> findByIdForUpdate(@Param("id") Long id);

    Set<Refund> findByPaymentIdIn(Collection<Long> paymentIds);

    boolean existsByPaymentId(Long paymentId);
}
