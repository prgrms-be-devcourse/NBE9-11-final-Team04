package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import com.team04.domain.settlement.entity.SettlementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByIdempotencyKey(String idempotencyKey);

    Optional<Settlement> findByIdeaIdAndType(Long ideaId, SettlementType type);

    List<Settlement> findByIdeaIdOrderByCreatedAtDesc(Long ideaId);

    List<Settlement> findByStatus(SettlementStatus status);
}
