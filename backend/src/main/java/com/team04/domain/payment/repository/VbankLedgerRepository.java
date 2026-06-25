package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.VbankLedger;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VbankLedgerRepository extends JpaRepository<VbankLedger, Long> {

    Optional<VbankLedger> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VbankLedger v WHERE v.idempotencyKey = :idempotencyKey")
    Optional<VbankLedger> findByIdempotencyKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    Optional<VbankLedger> findTopByIdeaIdAndAffectsBalanceOrderByIdDesc(Long ideaId, Boolean affectsBalance);

    List<VbankLedger> findByIdeaIdOrderByIdDesc(Long ideaId);
}
