package com.team04.domain.verification.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 프로젝트 검증 요청의 현재 상태와 보완 일정을 저장하는 엔티티입니다. */
@Entity
@Table(name = "project_verification")
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

    @Column(nullable = false)
    private Integer resubmissionCount = 0;

    private LocalDateTime revisionDueAt;

    private LocalDateTime waitingUntil;

    /** 아이디어 식별자로 신규 검증 요청을 생성합니다. */
    public ProjectVerification(Long ideaId) {
        this.ideaId = ideaId;
    }

    /** 현재 상태에서 목표 상태로의 전이를 검증한 뒤 상태를 변경합니다. */
    public void changeStatus(VerificationStatus targetStatus) {
        this.status.validateTransitionTo(targetStatus);
        this.status = targetStatus;
    }

    /** 보완 요청 상태로 변경하고 보완 마감 시간을 기록합니다. */
    public void requestRevision(LocalDateTime revisionDueAt) {
        changeStatus(VerificationStatus.NEEDS_REVISION);
        this.revisionDueAt = revisionDueAt;
    }

    /** 재제출 횟수를 증가시키고 AI 검증 상태로 변경합니다. */
    public void resubmit() {
        changeStatus(VerificationStatus.AI_VERIFYING);
        this.resubmissionCount += 1;
        this.revisionDueAt = null;
    }

    /** 외부 검증 대기 종료 시간을 기록합니다. */
    public void updateWaitingUntil(LocalDateTime waitingUntil) {
        this.waitingUntil = waitingUntil;
    }
}
