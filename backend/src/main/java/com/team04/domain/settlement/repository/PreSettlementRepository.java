package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreSettlementRepository extends JpaRepository<PreSettlement, Long> {

    List<PreSettlement> findByMilestoneId(Long milestoneId);

    /**
     * 프로젝트의 누적 선정산 금액을 조회합니다. (FAILED 제외)
     * Milestone 비관락으로 동시성 보장
     * 선정산 내역이 없으면 0 반환
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PreSettlement p " +
            "WHERE p.ideaId = :ideaId AND p.status != :failedStatus")
    Long sumAmountByIdeaIdAndStatusNot(
            @Param("ideaId") Long ideaId,
            @Param("failedStatus") PreSettlementStatus failedStatus);
}