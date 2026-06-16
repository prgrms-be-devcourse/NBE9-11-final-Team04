package com.team04.domain.verification.repository;

import com.team04.domain.verification.entity.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 항목별 검증 결과 이력의 저장과 조회를 담당하는 JPA 레포지토리입니다. */
public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {

    /** 아이디어 식별자로 검증 결과 이력 목록을 조회합니다. */
    List<VerificationResult> findAllByIdeaId(Long ideaId);
}
