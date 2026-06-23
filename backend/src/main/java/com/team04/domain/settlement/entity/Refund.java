package com.team04.domain.settlement.entity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 후원자별 환불 내역 엔티티입니다.
 * payment_id unique로 이중 환불을 방지합니다.
 */
@Entity
@Table(name = "refunds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    /**
     * 환불 대상 결제 — 후원자별 환불 금액 및 추적에 사용
     * unique: 하나의 결제에 대해 환불은 반드시 하나만 존재해야 함 (더블 환불 방지)
     */
    @Column(nullable = false, unique = true)
    private Long paymentId;

    @Column(nullable = false)
    private Long sponsorId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    /** PG 취소 거래 키 — 멱등 추적용 */
    private String pgCancelKey;

    /** PG 실패 사유 */
    private String failureReason;

    @Builder
    private Refund(Long ideaId, Long paymentId, Long sponsorId, Long amount, RefundReason reason) {
        this.ideaId = ideaId;
        this.paymentId = paymentId;
        this.sponsorId = sponsorId;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
    }

    public void complete(String pgCancelKey) {
        if (this.status != RefundStatus.PENDING) {
            throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
        }
        this.status = RefundStatus.COMPLETED;
        this.pgCancelKey = pgCancelKey;
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        if (this.status == RefundStatus.COMPLETED) {
            throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
        }
        this.status = RefundStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void retry() {
        if (this.status != RefundStatus.FAILED) {
            throw new CustomException(ErrorCode.REFUND_NOT_RETRYABLE);
        }
        this.status = RefundStatus.PENDING;
        this.failureReason = null;
    }
}
