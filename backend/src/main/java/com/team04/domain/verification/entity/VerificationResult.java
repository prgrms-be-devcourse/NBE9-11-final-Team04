package com.team04.domain.verification.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 항목별 프로젝트 검증 결과 이력을 저장하는 엔티티입니다. */
@Entity
@Table(
        name = "verification_result",
        indexes = @Index(name = "idx_verification_result_idea", columnList = "idea_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private VerificationCheckCode checkCode;

    @Column(nullable = false)
    private Boolean passed;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** 검증 항목 결과 이력을 생성합니다. */
    public VerificationResult(
            Long ideaId,
            VerificationCheckCode checkCode,
            Boolean passed,
            Integer score,
            String reason
    ) {
        this.ideaId = ideaId;
        this.checkCode = checkCode;
        this.passed = passed;
        this.score = score;
        this.reason = reason;
    }
}
