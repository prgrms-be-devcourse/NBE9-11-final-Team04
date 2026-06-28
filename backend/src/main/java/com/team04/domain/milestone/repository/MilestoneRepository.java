package com.team04.domain.milestone.repository;

import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.CompletionReportStatus;
import com.team04.domain.milestone.entity.MilestoneStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByIdeaIdOrderByStep(Long ideaId);

    /** 관리자 검토 대기 목록 — 제출된 보고서가 있는 마일스톤을 가장 오래된 제출 시각 기준으로 조회한다. */
    @Query("SELECT m FROM Milestone m JOIN CompletionReport r ON r.milestoneId = m.id " +
            "WHERE r.status = :status GROUP BY m ORDER BY MIN(r.submittedAt) ASC")
    List<Milestone> findPendingReportMilestonesOrderBySubmittedAtAsc(
            @Param("status") CompletionReportStatus status);

    /** 기한 초과 마일스톤 배치 처리용 — expectedDate가 기준일 이전이고 overdueAt이 null이며, 검토 대기 중인 보고서가 없는 마일스톤 조회 */
    @Query("SELECT m FROM Milestone m WHERE m.status = :status AND m.expectedDate < :date AND m.overdueAt IS NULL " +
            "AND NOT EXISTS (SELECT r FROM CompletionReport r WHERE r.milestoneId = m.id AND r.status = 'SUBMITTED')")
    List<Milestone> findByStatusAndExpectedDateBeforeAndOverdueAtIsNull(
            @Param("status") MilestoneStatus status,
            @Param("date") LocalDate date);

    /** 보증금 몰수 처리용 — overdueAt이 기준 시각 이전인 IN_PROGRESS 마일스톤 조회 */
    List<Milestone> findByStatusAndOverdueAtBeforeAndOverdueAtIsNotNull(MilestoneStatus status, LocalDateTime dateTime);

    Optional<Milestone> findByIdeaIdAndStep(Long ideaId, Integer step);

    Optional<Milestone> findByIdeaIdAndStatus(Long ideaId, MilestoneStatus status);

    /** 단계별 락 판단용 — 완료된 마일스톤 중 가장 높은 단계를 조회한다. */
    @Query("SELECT COALESCE(MAX(m.step), 0) FROM Milestone m WHERE m.ideaId = :ideaId AND m.status = :status")
    int findMaxStepByIdeaIdAndStatus(
            @Param("ideaId") Long ideaId,
            @Param("status") MilestoneStatus status);

    /** 선정산 동시성 제어를 위한 비관락 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Milestone m WHERE m.id = :id")
    Optional<Milestone> findByIdWithPessimisticLock(@Param("id") Long id);

    /** 자금 사용 내역 동시성 제어를 위한 비관락 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Milestone m WHERE m.ideaId = :ideaId AND m.status = :status")
    Optional<Milestone> findByIdeaIdAndStatusWithPessimisticLock(
            @Param("ideaId") Long ideaId,
            @Param("status") MilestoneStatus status);
}
