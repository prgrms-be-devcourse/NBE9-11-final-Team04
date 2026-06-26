package com.team04.domain.verification.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 프로젝트 검증 요청의 현재 상태를 저장하는 엔티티입니다. */
@Entity
@Table(
        name = "project_verification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_verification_idea",
                columnNames = "idea_id"
        ),
        indexes = @Index(name = "idx_project_verification_status", columnList = "status")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus status = VerificationStatus.DRAFT;

    /** 아이디어 식별자로 신규 검증 요청을 생성합니다. */
    public ProjectVerification(Long ideaId) {
        this.ideaId = ideaId;
    }

    /** 현재 상태에서 목표 상태로의 전이를 검증한 뒤 상태를 변경합니다. */
    public void changeStatus(VerificationStatus targetStatus) {
        this.status.validateTransitionTo(targetStatus);
        this.status = targetStatus;
    }

    /** 검증 시작 상태로 변경합니다. */
    public void startAiVerification() {
        changeStatus(VerificationStatus.AI_VERIFYING);
    }

    /** 참고용 검증 결과 저장이 완료된 상태로 변경합니다. */
    public void completeAiVerification() {
        changeStatus(VerificationStatus.AI_PASSED);
    }

    /** OpenAI 호출 실패 등 장애로 관리자 재시도가 필요한 상태로 변경합니다. */
    public void markPendingAdminReview() {
        changeStatus(VerificationStatus.PENDING_ADMIN_REVIEW);
    }
}
