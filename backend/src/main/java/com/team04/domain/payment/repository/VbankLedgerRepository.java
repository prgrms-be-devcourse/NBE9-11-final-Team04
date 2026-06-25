package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.VbankLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VbankLedgerRepository extends JpaRepository<VbankLedger, Long> {

    Optional<VbankLedger> findByIdempotencyKey(String idempotencyKey);

    /** 멱등키 중복 저장을 막기 위해 idea row 락 이후 기존 장부를 잠금 조회합니다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VbankLedger v WHERE v.idempotencyKey = :idempotencyKey")
    Optional<VbankLedger> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VbankLedger v WHERE v.ideaId = :ideaId AND v.affectsBalance = :affectsBalance ORDER BY v.id DESC")
    List<VbankLedger> findLatestByIdeaIdAndAffectsBalanceForUpdate(
            @Param("ideaId") Long ideaId,
            @Param("affectsBalance") Boolean affectsBalance,
            Pageable pageable
    );

    /** 최신순 장부 조회는 createdAt 동률 가능성을 피하기 위해 증가 PK 기준으로 정렬합니다. */
    List<VbankLedger> findByIdeaIdOrderByIdDesc(Long ideaId);
}
