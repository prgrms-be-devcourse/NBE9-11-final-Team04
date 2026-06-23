package com.team04.domain.match.repository;

import com.team04.domain.match.entity.ExpertMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpertMatchRepository extends JpaRepository<ExpertMatch, Long> {

    // 전문가 본인의 매칭 목록 조회
    @Query("SELECT m FROM ExpertMatch m JOIN FETCH m.expertProfile ep JOIN FETCH ep.user WHERE ep.user.id = :userId")
    List<ExpertMatch> findAllByUserId(@Param("userId") Long userId);

    // 매칭 단건 조회 (권한 검증 포함)
    @Query("SELECT m FROM ExpertMatch m JOIN FETCH m.expertProfile ep JOIN FETCH ep.user WHERE m.id = :matchId AND ep.user.id = :userId")
    Optional<ExpertMatch> findByIdAndUserId(@Param("matchId") Long matchId, @Param("userId") Long userId);

    // 동일한 아이디어-전문가 조합 중복 요청 방어
    boolean existsByIdeaIdAndExpertProfile_Id(Long ideaId, Long expertProfileId);

    // 매칭 단건 조회 (expertProfile.user LAZY 로딩 방지용 fetch join)
    @Query("SELECT m FROM ExpertMatch m JOIN FETCH m.expertProfile ep JOIN FETCH ep.user WHERE m.id = :id")
    Optional<ExpertMatch> findByIdWithProfile(@Param("id") Long id);
}