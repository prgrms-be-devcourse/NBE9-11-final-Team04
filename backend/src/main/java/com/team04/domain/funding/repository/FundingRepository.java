package com.team04.domain.funding.repository;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = "SELECT * FROM fundings WHERE id = :id FOR UPDATE", nativeQuery = true)
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

    /**
     * 결제 성공(PAID + SUCCESS) 후원자 여부 확인 — 워크스페이스 참여자 검증용
     */
    @Query("SELECT COUNT(f) > 0 FROM Funding f " +
            "JOIN Payment p ON p.fundingId = f.id " +
            "WHERE f.ideaId = :ideaId AND f.sponsorId = :sponsorId " +
            "AND f.status = com.team04.domain.funding.entity.FundingTypes.FundingStatus.PAID " +
            "AND p.status = com.team04.domain.payment.entity.PaymentTypes.PaymentStatus.SUCCESS")
    boolean existsPaidSponsorByIdeaIdAndSponsorId(@Param("ideaId") Long ideaId, @Param("sponsorId") Long sponsorId);

    /**
     * 마일스톤 전환 알림 대상 조회용
     * 실제 결제 성공 후원자만 중복 없이 반환합니다.
     */
    @Query("SELECT DISTINCT f.sponsorId FROM Funding f " +
            "JOIN Payment p ON p.fundingId = f.id " +
            "WHERE f.ideaId = :ideaId " +
            "AND f.status = com.team04.domain.funding.entity.FundingTypes.FundingStatus.PAID " +
            "AND p.status = com.team04.domain.payment.entity.PaymentTypes.PaymentStatus.SUCCESS")
    List<Long> findPaidSponsorIdsByIdeaId(@Param("ideaId") Long ideaId);
}
