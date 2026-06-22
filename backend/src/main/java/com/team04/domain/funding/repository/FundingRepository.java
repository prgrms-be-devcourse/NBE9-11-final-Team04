package com.team04.domain.funding.repository;

import com.team04.domain.funding.entity.Funding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;

public interface FundingRepository extends JpaRepository<Funding, Long> {

    Page<Funding> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Funding> findByIdeaIdOrderByCreatedAtDesc(Long ideaId, Pageable pageable);

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
}
