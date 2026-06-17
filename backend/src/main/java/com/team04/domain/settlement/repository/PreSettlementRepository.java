package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreSettlementRepository extends JpaRepository<PreSettlement, Long> {

    List<PreSettlement> findByMilestoneId(Long milestoneId);

    /** 마일스톤의 누적 선정산 금액을 조회합니다. (FAILED 제외) */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PreSettlement p " +
            "WHERE p.milestoneId = :milestoneId AND p.status != :status")
    Long sumAmountByMilestoneIdAndStatusNot(
            @Param("milestoneId") Long milestoneId,
            @Param("status") PreSettlementStatus status);

    /**
     * 프로젝트의 누적 선정산 금액을 조회합니다. (FAILED 제외)
     * 비관락으로 동시 요청 제어 — 한도 초과 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PreSettlement p " +
            "WHERE p.ideaId = :ideaId AND p.status != :status")
    Long sumAmountByIdeaIdAndStatusNot(
            @Param("ideaId") Long ideaId,
            @Param("status") PreSettlementStatus status);
}