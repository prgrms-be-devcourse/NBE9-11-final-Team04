package com.team04.domain.funding.repository;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FundingRepository extends JpaRepository<Funding, Long> {

    Page<Funding> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Funding> findByIdeaIdOrderByCreatedAtDesc(Long ideaId, Pageable pageable);

    /** 환불 후원자 목록 일괄 조회용 — 페이징 없이 전체 반환 */
    List<Funding> findAllByIdeaId(Long ideaId);

    Optional<Funding> findFirstByIdeaIdAndSponsorIdAndStatusInOrderByCreatedAtDesc(
            Long ideaId,
            Long sponsorId,
            Collection<FundingStatus> statuses
    );

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Funding f WHERE f.ideaId = :ideaId AND f.status = :status")
    Long sumAmountByIdeaIdAndStatus(@Param("ideaId") Long ideaId, @Param("status") FundingStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Funding f WHERE f.id = :id")
    Optional<Funding> findByIdForUpdate(@Param("id") Long id);

    /**
     * 후원자 접근 권한 체크용
     * 환불된 후원자는 후원자로 인정하지 않음 — REFUNDED 상태 제외
     * Hibernate 6 타입 검증 오류 방지를 위해 fully qualified enum path 사용
     */
    @Query("SELECT COUNT(f) > 0 FROM Funding f " +
            "JOIN Payment p ON p.fundingId = f.id " +
            "WHERE f.ideaId = :ideaId AND f.sponsorId = :sponsorId " +
            "AND p.status != com.team04.domain.payment.entity.PaymentTypes.PaymentStatus.REFUNDED")
    boolean existsByIdeaIdAndSponsorId(@Param("ideaId") Long ideaId, @Param("sponsorId") Long sponsorId);
}