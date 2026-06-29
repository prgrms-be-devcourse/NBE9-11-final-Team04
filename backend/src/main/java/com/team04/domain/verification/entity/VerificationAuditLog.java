package com.team04.domain.verification.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 프로젝트 검증 상태 변경 이력을 감사 목적으로 저장하는 엔티티입니다. */
@Entity
@Table(
        name = "verification_audit_log",
        indexes = {
                @Index(name = "idx_verification_audit_log_idea", columnList = "idea_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus nextStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** 상태 변경 전후 상태와 사유를 포함한 감사 로그를 생성합니다. */
    public VerificationAuditLog(
            Long ideaId,
            VerificationStatus previousStatus,
            VerificationStatus nextStatus,
            String reason
    ) {
        this.ideaId = ideaId;
        this.previousStatus = previousStatus;
        this.nextStatus = nextStatus;
        this.reason = reason;
    }
}
