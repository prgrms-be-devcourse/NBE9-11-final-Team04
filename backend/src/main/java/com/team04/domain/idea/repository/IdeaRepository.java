package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 아이디어 엔티티의 저장과 조회를 담당하는 JPA 레포지토리입니다. */
public interface IdeaRepository extends JpaRepository<Idea, Long>, IdeaRepositoryCustom {

    /** 소프트 삭제되지 않은 아이디어를 식별자로 조회합니다. */
    Optional<Idea> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Idea i WHERE i.id = :id AND i.deletedAt IS NULL")
    Optional<Idea> findByIdForUpdate(@Param("id") Long id);

    /** 특정 사용자가 등록한 소프트 삭제되지 않은 아이디어 목록을 최신순으로 조회합니다. */
    List<Idea> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    // [notification] 마감임박 알림 스케줄러에서 사용 — 목표금액 미달성이면서 마감일이 from~to 사이인 아이디어 조회
    @Query("SELECT i FROM Idea i WHERE i.status = :status AND i.fundingEndAt >= :from AND i.fundingEndAt < :to AND i.currentAmount < i.goalAmount AND i.deletedAt IS NULL")
    List<Idea> findClosingIdeas(@Param("status") IdeaStatus status,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    Page<Idea> findByStatusInAndDeletedAtIsNull(Collection<IdeaStatus> statuses, Pageable pageable);
}
