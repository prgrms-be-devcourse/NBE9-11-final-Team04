package com.team04.domain.idea.repository;

import com.team04.domain.idea.entity.IdeaBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 관심 프로젝트 저장, 삭제, 본인 목록 조회를 담당하는 레포지토리입니다. */
public interface IdeaBookmarkRepository extends JpaRepository<IdeaBookmark, Long> {

    /** 사용자가 이미 해당 아이디어를 북마크했는지 확인합니다. */
    boolean existsByUserIdAndIdeaId(Long userId, Long ideaId);

    /** 로그인 사용자의 북마크를 최신순 Page 페이지로 조회합니다. */
    Page<IdeaBookmark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM IdeaBookmark b WHERE b.userId = :userId AND b.ideaId = :ideaId")
    int deleteByUserIdAndIdeaIdBulk(@Param("userId") Long userId, @Param("ideaId") Long ideaId);
}
