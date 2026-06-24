package com.team04.domain.settlement.entity;

import com.team04.global.entity.BaseEntity;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 후원자별 환불 내역 엔티티입니다.
 *
 * settlement_id 제거 이유:
 *   환불 추적은 payment_id 하나로 충분합니다.
 *   payment → fundingId, amount 흐름으로 조회 가능하며,
 *   ideaId 기준으로 Settlement도 조회할 수 있어 settlement_id의 중복 참조가 불필요합니다.
 *   추후 Settlement와의 연결이 필요한 요구사항이 생기면 재검토합니다.
 *
 * // private Long settlementId; // 위 이유로 제거
 */
@Entity
@Table(name = "refunds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 환불 대상 결제 — 후원자별 환불 금액 및 추적에 사용
     * unique: 하나의 결제에 대해 환불은 반드시 하나만 존재해야 함 (더블 환불 방지)
     */
    @Column(nullable = false, unique = true)
    private Long paymentId;

    /** 환불 대상 후원자 — payment.fundingId → funding.sponsorId 흐름으로 조회하여 저장 */
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

    @Builder
    private Refund(Long paymentId, Long sponsorId, Long amount, RefundReason reason) {
        this.paymentId = paymentId;
        this.sponsorId = sponsorId;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
    }

    /**
     * 환불 완료 처리합니다.
     * PENDING 상태에서만 가능합니다.
     */
    public void complete() {
        if (this.status == RefundStatus.COMPLETED) {
            return;
        }
        if (this.status != RefundStatus.PENDING) {
            throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
        }
        this.status = RefundStatus.COMPLETED;
    }

    /**
     * 환불 실패 처리합니다.
     * PENDING 상태에서만 가능합니다.
     */
    public void fail() {
        if (this.status == RefundStatus.FAILED) {
            return;
        }
        if (this.status != RefundStatus.PENDING) {
            throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
        }
        this.status = RefundStatus.FAILED;
    }
}
