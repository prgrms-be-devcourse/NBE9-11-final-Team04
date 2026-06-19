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
        validatePendingStatus();
        this.status = SettlementStatus.COMPLETED;
    }

    public void partialRefund() {
        validatePendingStatus();
        this.status = SettlementStatus.PARTIALLY_REFUNDED;
    }

    public void forfeit() {
        validatePendingStatus();
        this.status = SettlementStatus.FORFEITED;
    }

    public void refund() {
        validatePendingStatus();
        this.status = SettlementStatus.REFUNDED;
    }

    private void validatePendingStatus() {
        if (this.status != SettlementStatus.PENDING) {
            throw new CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS_TRANSITION);
        }
    }
}