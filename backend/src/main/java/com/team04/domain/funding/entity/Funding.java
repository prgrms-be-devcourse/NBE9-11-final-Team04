package com.team04.domain.funding.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.domain.idea.entity.RewardType;
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

@Entity
@Table(name = "fundings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Funding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Long sponsorId;

    @Column(nullable = false)
    private Integer milestoneStep;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardType rewardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FundingTypes.FundingStatus status;

    private LocalDateTime refundedAt;

    /** Idea.currentAmount 반영 여부 — FundingPaidEvent 중복 수신 멱등용 */
    @Column(nullable = false)
    private boolean amountAppliedToIdea = false;

    public static Funding createPending(Long ideaId, Long sponsorId, Integer milestoneStep,
                                        Long amount, RewardType rewardType) {
        Funding funding = new Funding();
        funding.ideaId = ideaId;
        funding.sponsorId = sponsorId;
        funding.milestoneStep = milestoneStep;
        funding.amount = amount;
        funding.rewardType = rewardType;
        funding.status = FundingTypes.FundingStatus.PENDING_PAYMENT;
        return funding;
    }

    public void markAsPaid() {
        if (this.status != FundingTypes.FundingStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.status = FundingTypes.FundingStatus.PAID;
    }

    public void markAsRefunded() {
        if (this.status != FundingTypes.FundingStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.status = FundingTypes.FundingStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }

    /** 결제 실패·만료·후원 철회 시 PENDING_PAYMENT → CANCELLED */
    public void markAsCancelled() {
        if (this.status != FundingTypes.FundingStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.status = FundingTypes.FundingStatus.CANCELLED;
    }

    /**
     * 후원 금액이 아이디어 누적 후원금에 반영되었음을 표시합니다.
     * {@link com.team04.domain.idea.event.IdeaFundingPaidListener}에서 1회만 호출됩니다.
     */
    public void markAmountAppliedToIdea() {
        this.amountAppliedToIdea = true;
    }
}
