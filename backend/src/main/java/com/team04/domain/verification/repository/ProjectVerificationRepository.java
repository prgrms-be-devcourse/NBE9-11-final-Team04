package com.team04.domain.verification.repository;

import com.team04.domain.verification.entity.ProjectVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** 프로젝트 검증 요청 엔티티의 저장과 조회를 담당하는 JPA 레포지토리입니다. */
public interface ProjectVerificationRepository extends JpaRepository<ProjectVerification, Long> {

    /** 아이디어 식별자로 프로젝트 검증 요청을 조회합니다. */
    Optional<ProjectVerification> findByIdeaId(Long ideaId);
}
