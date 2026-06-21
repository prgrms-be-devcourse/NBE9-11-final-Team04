package com.team04.domain.milestone.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Milestone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Integer step;

    @Column(nullable = false)
    private String goal;

    @Column(columnDefinition = "TEXT")
    private String expectedResult;

    @Column
    private LocalDate expectedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status;

    /**
     * 기한 초과 감지 시각
     * null이면 정상 진행 중, 값이 있으면 기한 초과 상태
     * 소명 보고서 제출 시 null로 초기화 (먹튀 아님으로 판단)
     * 3일 경과 후에도 소명 보고서 없으면 보증금 몰수 처리
     */
    @Column
    private LocalDateTime overdueAt;

    @Builder
    private Milestone(Long ideaId, Integer step, String goal,
                      String expectedResult, LocalDate expectedDate) {
        if (step == null || step < 1 || step > 3) {
            throw new IllegalArgumentException("마일스톤 단계는 1~3 사이여야 합니다");
        }
        this.ideaId = ideaId;
        this.step = step;
        this.goal = goal;
        this.expectedResult = expectedResult;
        this.expectedDate = expectedDate;
        this.status = MilestoneStatus.PENDING;
    }

    /** 마일스톤을 진행 중 상태로 전이합니다. */
    public void start() {
        this.status.validateTransitionTo(MilestoneStatus.IN_PROGRESS);
        this.status = MilestoneStatus.IN_PROGRESS;
    }

    /** 마일스톤을 완료 상태로 전이합니다. */
    public void complete() {
        this.status.validateTransitionTo(MilestoneStatus.COMPLETED);
        this.status = MilestoneStatus.COMPLETED;
    }

    /** 마일스톤을 취소 상태로 전이합니다. */
    public void cancel() {
        this.status.validateTransitionTo(MilestoneStatus.CANCELLED);
        this.status = MilestoneStatus.CANCELLED;
    }

    /**
     * 기한 초과 시각을 기록합니다.
     * MilestoneScheduler에서 기한 초과 감지 시 호출합니다.
     */
    public void markOverdue(LocalDateTime overdueAt) {
        this.overdueAt = overdueAt;
    }

    /**
     * 기한 초과 시각을 초기화합니다.
     * 소명 보고서 제출 시 호출 — 먹튀 아님으로 판단합니다.
     */
    public void clearOverdue() {
        this.overdueAt = null;
    }
}