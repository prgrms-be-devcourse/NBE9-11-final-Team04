package com.team04.domain.idea.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
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

/** 아이디어 기본 정보, 심사 상태, 펀딩 수치를 저장하는 엔티티입니다. */
@Entity
@Table(name = "idea")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Idea extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdeaCategory category;

    @Column(nullable = false, length = 200)
    private String oneLineIntro;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String problemDefinition;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String solution;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String goal;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String targetCustomer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String competitor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String teamIntro;

    @Column(nullable = false)
    private Long goalAmount;

    @Column(nullable = false)
    private Long depositAmount;

    @Column(nullable = false)
    private Long currentAmount = 0L;

    @Column(nullable = false)
    private int sponsorCount = 0;

    @Column(nullable = false)
    private LocalDateTime fundingStartAt;

    @Column(nullable = false)
    private LocalDateTime fundingEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RewardType rewardType;

    @Column(length = 2048)
    private String imageUrl; // 대표 이미지

    @Column(columnDefinition = "TEXT")
    private String imageUrls; // 본문 이미지

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdeaStatus status = IdeaStatus.AI_PENDING;

    @Column(nullable = false)
    private Integer trustScore = 0;

    @Column(length = 500)
    private String rejectReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdeaBadge badge = IdeaBadge.NO_HISTORY;

    private LocalDateTime deletedAt;

    // 분쟁 처리 중 일시 중단 전 상태를 저장합니다. 소명 수용 시 이 값으로 복원합니다.
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private IdeaStatus previousStatus;

    /** 신규 아이디어를 심사 대기 기본값과 함께 생성합니다. */
    public Idea(
            Long userId,
            String title,
            IdeaCategory category,
            String oneLineIntro,
            String problemDefinition,
            String solution,
            String goal,
            String targetCustomer,
            String competitor,
            String teamIntro,
            Long goalAmount,
            Long depositAmount,
            LocalDateTime fundingStartAt,
            LocalDateTime fundingEndAt,
            RewardType rewardType,
            String imageUrl,
            String imageUrls
    ) {
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.oneLineIntro = oneLineIntro;
        this.problemDefinition = problemDefinition;
        this.solution = solution;
        this.goal = goal;
        this.targetCustomer = targetCustomer;
        this.competitor = competitor;
        this.teamIntro = teamIntro;
        this.goalAmount = goalAmount;
        this.depositAmount = depositAmount;
        this.fundingStartAt = fundingStartAt;
        this.fundingEndAt = fundingEndAt;
        this.rewardType = rewardType;
        this.imageUrl = imageUrl;
        this.imageUrls = imageUrls;
    }

    /** 요청한 사용자가 아이디어 작성자인지 검증합니다. */
    public void validateOwner(Long requestUserId) {
        if (!this.userId.equals(requestUserId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    /** 심사 대기 상태에서만 아이디어 내용을 수정합니다. */
    public void update(
            String title,
            IdeaCategory category,
            String oneLineIntro,
            String problemDefinition,
            String solution,
            String goal,
            String targetCustomer,
            String competitor,
            String teamIntro,
            Long goalAmount,
            LocalDateTime fundingStartAt,
            LocalDateTime fundingEndAt,
            RewardType rewardType,
            String imageUrl
    ) {
        validateEditable();
        this.title = title;
        this.category = category;
        this.oneLineIntro = oneLineIntro;
        this.problemDefinition = problemDefinition;
        this.solution = solution;
        this.goal = goal;
        this.targetCustomer = targetCustomer;
        this.competitor = competitor;
        this.teamIntro = teamIntro;
        this.goalAmount = goalAmount;
        this.fundingStartAt = fundingStartAt;
        this.fundingEndAt = fundingEndAt;
        this.imageUrl = imageUrl;
    }

    /** 심사 대기 상태에서만 아이디어 대표 이미지를 변경합니다. */
    public void updateImageUrl(String imageUrl) {
        validateEditable();
        this.imageUrl = imageUrl;
    }

    /** 심사 대기 상태에서만 본문 이미지 URL 목록 문자열을 변경합니다. */
    public void updateImageUrls(String imageUrls) {
        validateEditable();
        this.imageUrls = imageUrls;
    }

    /** 심사 대기 상태에서만 아이디어를 소프트 삭제합니다. */
    public void softDelete() {
        validateDeletable();
        this.deletedAt = LocalDateTime.now();
    }

    /** 결제 완료 이벤트 금액을 현재 모금액에 더합니다. */
    public void addCurrentAmount(Long amount) {
        this.currentAmount += amount;
    }

    /** 첫 후원자 결제 완료 시 인기 점수 계산용 후원자 수를 증가시킵니다. */
    public void increaseSponsorCount() {
        this.sponsorCount++;
    }

    /** 마지막 후원 취소 시 인기 점수 계산용 후원자 수를 0 아래로 내려가지 않게 감소시킵니다. */
    public void decreaseSponsorCount() {
        if (this.sponsorCount > 0) {
            this.sponsorCount--;
        }
    }

    /** 진행 중인 펀딩 아이디어에 대해 제안자 취소 신청 상태로 전이합니다. */
    public void requestCancellation() {
        if (this.status != IdeaStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
        }
        this.status = IdeaStatus.CANCELLATION_REQUESTED;
    }

    /** 아이디어 상태 전이를 열거형 규칙에 따라 수행합니다. */
    public void changeStatus(IdeaStatus targetStatus) {
        this.status.validateTransitionTo(targetStatus);
        this.status = targetStatus;
    }

    /** 아이디어 검증 이력 또는 인증 상태 배지를 변경합니다. */
    public void changeBadge(IdeaBadge badge) {
        this.badge = badge;
    }

    /** 이미 소프트 삭제된 아이디어인지 확인합니다. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 후원 결제 완료 시 누적 후원금과 후원자 수를 갱신합니다. */
    public void addFundingAmount(Long amount) {
        this.currentAmount += amount;
        this.sponsorCount++;
    }

    /** 후원 환불 시 누적 후원금과 후원자 수를 갱신합니다. */
    public void subtractFundingAmount(Long amount) {
        this.currentAmount = Math.max(0L, this.currentAmount - amount);
        decreaseSponsorCount();
    }

    /** 현재 아이디어가 수정 가능한 상태인지 검증합니다. */
    public void validateEditable() {
        if (!this.status.isEditable()) {
            throw new CustomException(ErrorCode.IDEA_STATUS_NOT_EDITABLE);
        }
    }

    /** 현재 아이디어가 삭제 가능한 상태인지 검증합니다. */
    private void validateDeletable() {
        if (!this.status.isDeletable()) {
            throw new CustomException(ErrorCode.IDEA_STATUS_NOT_DELETABLE);
        }
    }

    /** AI 검증 완료 후 전문가 심사 대기 상태로 전이합니다. */
    public void completeAiVerification() {
        changeStatus(IdeaStatus.EXPERT_PENDING);
    }

    /** 전문가 검토 완료 후 관리자 심사 대기 상태로 전이합니다. */
    public void completeExpertReview() {
        changeStatus(IdeaStatus.ADMIN_PENDING);
    }

    /** 관리자 승인 후 펀딩 공개 상태로 전이합니다. */
    public void open() {
        changeStatus(IdeaStatus.OPEN);
    }

    /** 관리자 반려 후 반려 사유를 저장하고 반려 상태로 전이합니다. */
    public void reject(String reason) {
        this.rejectReason = reason;
        changeStatus(IdeaStatus.REJECTED);
    }

    // 분쟁 신고 처리 중 관리자가 호출합니다. 복원 시 사용할 이전 상태를 저장합니다.
    /** 관리자 일시 중단 처리합니다. */
    public void suspend() {
        this.previousStatus = this.status;
        changeStatus(IdeaStatus.SUSPENDED);
    }

    // 소명 수용(DisputeStatus.REJECTED) 시 호출됩니다. previousStatus로 복원합니다.
    /** 관리자 일시 중단 해제 후 중단 전 상태로 복원합니다. */
    public void restore() {
        if (this.status != IdeaStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
        }
        IdeaStatus target = this.previousStatus != null ? this.previousStatus : IdeaStatus.OPEN;
        this.previousStatus = null;
        changeStatus(target);
    }
}