package com.team04.domain.settlement.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 마일스톤 진행 중 제안자가 신청하는 선정산 엔티티입니다. */
@Entity
@Table(name = "pre_settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreSettlementStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Builder
    private PreSettlement(Long ideaId, Long amount) {
        this.ideaId = ideaId;
        this.amount = amount;
        this.status = PreSettlementStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    /**
     * 선정산 지급을 완료 처리합니다.
     * REQUESTED 상태에서만 가능합니다.
     */
    public void complete() {
        if (this.status == PreSettlementStatus.COMPLETED) {
            return;
        }
        if (this.status != PreSettlementStatus.REQUESTED) {
            throw new CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS_TRANSITION);
        }
        this.status = PreSettlementStatus.COMPLETED;
    }

    /**
     * 선정산 지급을 실패 처리합니다.
     * REQUESTED 상태에서만 가능합니다.
     */
    public void fail() {
        if (this.status == PreSettlementStatus.FAILED) {
            return;
        }
        if (this.status != PreSettlementStatus.REQUESTED) {
            throw new CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS_TRANSITION);
        }
        this.status = PreSettlementStatus.FAILED;
    }

    /** 지급 실패 건을 스케줄러가 다시 처리할 수 있도록 대기 상태로 되돌립니다. */
    public void retry() {
        // 스케줄러 재처리는 실패 건만 대상으로 하여 완료 건의 중복 지급을 막는다.
        if (this.status != PreSettlementStatus.FAILED) {
            throw new CustomException(ErrorCode.SETTLEMENT_INVALID_STATUS_TRANSITION);
        }
        this.status = PreSettlementStatus.REQUESTED;
    }
}
