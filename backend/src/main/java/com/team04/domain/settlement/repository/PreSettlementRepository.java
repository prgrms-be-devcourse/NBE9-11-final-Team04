package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.PreSettlement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreSettlementRepository extends JpaRepository<PreSettlement, Long> {

    List<PreSettlement> findByMilestoneId(Long milestoneId);

    /**
     * 프로젝트의 누적 선정산 금액을 조회합니다.
     * FAILED 제외, accumulatedAmount의 MAX값으로 현재 누적 금액 파악
     * 비관락으로 동시 요청 제어 — 한도 초과 방지
     * 선정산 내역이 없으면 0 반환
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COALESCE(MAX(p.accumulatedAmount), 0) FROM PreSettlement p " +
            "WHERE p.ideaId = :ideaId AND p.status != 'FAILED'")
    Long findMaxAccumulatedAmountByIdeaId(@Param("ideaId") Long ideaId);
}