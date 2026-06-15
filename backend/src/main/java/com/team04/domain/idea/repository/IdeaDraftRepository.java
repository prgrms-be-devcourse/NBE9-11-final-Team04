package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.IdeaDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/** 아이디어 임시저장 엔티티의 저장과 조회를 담당하는 JPA 레포지토리입니다. */
public interface IdeaDraftRepository extends JpaRepository<IdeaDraft, Long> {

    /** 보관 기간 내 특정 사용자의 임시저장 목록을 최신 수정순으로 조회합니다. */
    List<IdeaDraft> findByUserIdAndUpdatedAtAfterOrderByUpdatedAtDesc(Long userId, LocalDateTime retentionStartAt);

    /** 특정 사용자의 보관 기간 내 임시저장 개수를 조회합니다. */
    long countByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime retentionStartAt);
}
