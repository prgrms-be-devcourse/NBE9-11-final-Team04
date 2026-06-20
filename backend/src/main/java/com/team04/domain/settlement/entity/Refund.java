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
 *   payment → fundingId, amount, sponsorId 흐름으로 "누가 얼마를 후원했는지" 모두 조회 가능하며,
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

    /** 환불 대상 결제 — 후원자별 환불 금액 및 추적에 사용 */
    @Column(nullable = false)
    private Long paymentId;

    /** 환불 대상 후원자 — payment 엔티티에 sponsorId가 없어 별도 보관 */
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
        if (this.status != RefundStatus.PENDING) {
            throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
        }
        this.status = RefundStatus.COMPLETED;
    }
}