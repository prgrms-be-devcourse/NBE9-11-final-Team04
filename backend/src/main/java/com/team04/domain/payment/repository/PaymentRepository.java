package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByFundingIdAndStatus(Long fundingId, PaymentStatus status);

    boolean existsByFundingIdAndStatusAndIdNot(Long fundingId, PaymentStatus status, Long id);

    List<Payment> findByFundingIdOrderByCreatedAtDesc(Long fundingId);

    Optional<Payment> findByOrderId(String orderId);

    @Query("SELECT p.fundingId FROM Payment p WHERE p.id = :paymentId")
    Optional<Long> findFundingIdById(@Param("paymentId") Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    Optional<Payment> findFirstByFundingIdAndStatusOrderByCreatedAtDesc(Long fundingId, PaymentStatus status);

    @Query(value = "SELECT * FROM payments WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            SELECT * FROM payments
            WHERE funding_id = :fundingId AND status = :status
            ORDER BY created_at DESC
            LIMIT 1
            FOR UPDATE
            """, nativeQuery = true)
    Optional<Payment> findFirstByFundingIdAndStatusForUpdate(
            @Param("fundingId") Long fundingId,
            @Param("status") String status
    );

    @Query("""
            SELECT p FROM Payment p
            WHERE p.fundingId IN (
                SELECT f.id FROM Funding f WHERE f.sponsorId = :sponsorId
            )
            ORDER BY p.createdAt DESC
            """)
    Page<Payment> findBySponsorId(@Param("sponsorId") Long sponsorId, Pageable pageable);

    /**
     * 환불 대상 결제 + 후원자 ID 일괄 조회 (N+1 해결)
     * SUCCESS 상태이고 아직 환불 레코드가 없는 결제만 조회
     * Refund LEFT JOIN으로 중복 환불 방지
     */
    @Query("SELECT p, f.sponsorId FROM Payment p " +
            "JOIN Funding f ON p.fundingId = f.id " +
            "LEFT JOIN Refund r ON r.paymentId = p.id " +
            "WHERE f.ideaId = :ideaId " +
            "AND p.status = com.team04.domain.payment.entity.PaymentTypes.PaymentStatus.SUCCESS " +
            "AND r.id IS NULL")
    List<Object[]> findPaymentsAndSponsorIdsToRefund(@Param("ideaId") Long ideaId);
}
