package com.team04.domain.verification.repository;

import com.team04.domain.verification.entity.VerificationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 검증 상태 변경 감사 로그의 저장과 조회를 담당하는 JPA 레포지토리입니다. */
public interface VerificationAuditLogRepository extends JpaRepository<VerificationAuditLog, Long> {

    /** 아이디어 식별자로 검증 상태 변경 감사 로그 목록을 조회합니다.*/
    List<VerificationAuditLog> findAllByIdeaId(Long ideaId);
}
