package com.team04.domain.settlement.entity;

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
    private Long milestoneId;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Long amount;

    /**
     * 이 선정산 시점까지의 누적 선정산 금액 (FAILED 제외)
     * 직전 성공한 선정산의 accumulatedAmount + 현재 신청 금액으로 계산
     * FAILED 시 이전 성공 기준값 그대로 유지
     * MAX(accumulatedAmount)로 현재 누적 금액 조회 가능
     */
    @Column(nullable = false)
    private Long accumulatedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PreSettlementStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Builder
    private PreSettlement(Long milestoneId, Long ideaId, Long amount, Long accumulatedAmount) {
        this.milestoneId = milestoneId;
        this.ideaId = ideaId;
        this.amount = amount;
        this.accumulatedAmount = accumulatedAmount;
        this.status = PreSettlementStatus.REQUESTED;
        this.requestedAt = LocalDateTime.now();
    }

    /** 선정산 지급을 완료 처리합니다. */
    public void complete() {
        this.status = PreSettlementStatus.COMPLETED;
    }

    /** 선정산 지급을 실패 처리합니다. */
    public void fail() {
        this.status = PreSettlementStatus.FAILED;
    }
}