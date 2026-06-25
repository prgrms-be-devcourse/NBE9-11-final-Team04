package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreSettlementRepository extends JpaRepository<PreSettlement, Long> {

    List<PreSettlement> findByIdeaId(Long ideaId);

    List<PreSettlement> findByStatus(PreSettlementStatus status);

    /**
     * 프로젝트의 유효 누적 선정산 금액 조회 (FAILED 제외)
     * Milestone 비관락으로 동시성 보장
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PreSettlement p " +
            "WHERE p.ideaId = :ideaId AND p.status != :failedStatus")
    Long sumAmountByIdeaIdAndStatusNot(
            @Param("ideaId") Long ideaId,
            @Param("failedStatus") PreSettlementStatus failedStatus);

    /**
     * 실제 지급 완료된 선정산 누적액 조회 (COMPLETED만)
     * 자금 사용 내역 입력 시 수령액 초과 방지에 사용
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PreSettlement p " +
            "WHERE p.ideaId = :ideaId AND p.status = :completedStatus")
    Long sumAmountByIdeaIdAndStatus(
            @Param("ideaId") Long ideaId,
            @Param("completedStatus") PreSettlementStatus completedStatus);
}
