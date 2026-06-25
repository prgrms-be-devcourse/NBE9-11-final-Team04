package com.team04.domain.payment.repository;

import com.team04.domain.payment.entity.VbankLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VbankLedgerRepository extends JpaRepository<VbankLedger, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<VbankLedger> findByIdempotencyKey(String idempotencyKey);

    Optional<VbankLedger> findTopByIdeaIdAndAffectsBalanceOrderByIdDesc(Long ideaId, Boolean affectsBalance);

    List<VbankLedger> findByIdeaIdOrderByCreatedAtDesc(Long ideaId);
}
