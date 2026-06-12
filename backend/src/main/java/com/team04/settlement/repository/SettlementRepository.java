package com.team04.settlement.repository;

import com.team04.settlement.entity.Settlement;
import com.team04.settlement.entity.SettlementTypes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByIdempotencyKey(String idempotencyKey);

    Optional<Settlement> findByIdeaIdAndType(Long ideaId, SettlementTypes.SettlementType type);
}