package com.team04.domain.settlement.repository;

import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreSettlementRepository extends JpaRepository<PreSettlement, Long> {

    List<PreSettlement> findByIdeaId(Long ideaId);

    List<PreSettlement> findByStatus(PreSettlementStatus status);

    /**
     * 외부 지급 API 호출 전 REQUESTED 건을 PROCESSING으로 선점합니다.
     * 영향받은 행이 1개일 때만 해당 스케줄러 인스턴스가 지급을 진행합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PreSettlement p SET p.status = :processingStatus " +
            "WHERE p.id = :preSettlementId AND p.status = :requestedStatus")
    int markProcessingIfRequested(
            @Param("preSettlementId") Long preSettlementId,
            @Param("requestedStatus") PreSettlementStatus requestedStatus,
            @Param("processingStatus") PreSettlementStatus processingStatus);

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
