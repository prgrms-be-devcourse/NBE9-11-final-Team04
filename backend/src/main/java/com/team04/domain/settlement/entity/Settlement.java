package com.team04.domain.settlement.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementType type;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private Long platformFee;

    @Column(nullable = false)
    private Long payoutAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Builder
    private Settlement(Long ideaId, SettlementType type,
                       Long totalAmount, Long platformFee, Long payoutAmount,
                       String idempotencyKey) {
        if (platformFee + payoutAmount > totalAmount) {
            throw new IllegalArgumentException("platformFee와 payoutAmount의 합은 totalAmount를 초과할 수 없습니다");
        }
        this.ideaId = ideaId;
        this.type = type;
        this.totalAmount = totalAmount;
        this.platformFee = platformFee;
        this.payoutAmount = payoutAmount;
        this.status = SettlementStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
    }

    public void complete() {
        completeAs(SettlementStatus.COMPLETED);
    }

    public void depositRefund() {
        completeAs(SettlementStatus.DEPOSIT_REFUNDED);
    }

    public void depositExhausted() {
        completeAs(SettlementStatus.DEPOSIT_EXHAUSTED);
    }

    public void partialRefund() {
        completeAs(SettlementStatus.PARTIALLY_REFUNDED);
    }

    public void forfeit() {
        completeAs(SettlementStatus.FORFEITED);
    }

    public void refund() {
        completeAs(SettlementStatus.REFUNDED);
    }

    public void fail() {
        if (this.status == SettlementStatus.FAILED) {
            return;
        }
        validatePendingStatus();
        this.status = SettlementStatus.FAILED;
    }

    /** 지급 실패 건을 스케줄러가 다시 처리할 수 있도록 대기 상태로 되돌립니다. */
    public void retryPayout() {
        // 스케줄러 재처리는 실패 건만 대상으로 하여 완료 장부의 중복 지급을 막는다.
        if (this.status != SettlementStatus.FAILED) {
            throw new CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS_TRANSITION);
        }
        this.status = SettlementStatus.PENDING;
    }

    public void completeAs(SettlementStatus successStatus) {
        validatePendingStatus();
        if (successStatus == SettlementStatus.PENDING || successStatus == SettlementStatus.FAILED) {
            throw new CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS_TRANSITION);
        }
        this.status = successStatus;
    }

    private void validatePendingStatus() {
        if (this.status != SettlementStatus.PENDING) {
            throw new CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS_TRANSITION);
        }
    }
}
