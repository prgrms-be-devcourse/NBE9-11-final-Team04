package com.team04.domain.verification.repository;

import com.team04.domain.verification.entity.TrustScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 신뢰도 점수 엔티티의 저장과 조회를 담당하는 JPA 레포지토리입니다. */
public interface TrustScoreRepository extends JpaRepository<TrustScore, Long> {

    /** 아이디어 식별자로 신뢰도 점수를 조회합니다. */
    Optional<TrustScore> findByIdeaId(Long ideaId);
}
