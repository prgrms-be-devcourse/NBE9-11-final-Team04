package com.team04.domain.milestone.repository;

import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByIdeaIdOrderByStep(Long ideaId);

    Optional<Milestone> findByIdeaIdAndStep(Long ideaId, Integer step);

    Optional<Milestone> findByIdeaIdAndStatus(Long ideaId, MilestoneStatus status);

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