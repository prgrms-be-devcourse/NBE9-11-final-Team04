package com.team04.domain.payment.repository;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByFundingIdAndStatus(Long fundingId, PaymentStatus status);
    List<Payment> findByFundingIdOrderByCreatedAtDesc(Long fundingId);
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findFirstByFundingIdAndStatusOrderByCreatedAtDesc(Long fundingId, PaymentStatus status);

    @Query("""
            SELECT p FROM Payment p
            WHERE p.fundingId IN (
                SELECT f.id FROM Funding f WHERE f.sponsorId = :sponsorId
            )
            ORDER BY p.createdAt DESC
            """)
    Page<Payment> findBySponsorId(@Param("sponsorId") Long sponsorId, Pageable pageable);
}
