package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.Idea;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** 아이디어 엔티티의 저장과 조회를 담당하는 JPA 레포지토리입니다. */
public interface IdeaRepository extends JpaRepository<Idea, Long>, IdeaRepositoryCustom {

    /** 소프트 삭제되지 않은 아이디어를 식별자로 조회합니다. */
    Optional<Idea> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Idea i WHERE i.id = :id AND i.deletedAt IS NULL")
    Optional<Idea> findByIdForUpdate(@Param("id") Long id);
}