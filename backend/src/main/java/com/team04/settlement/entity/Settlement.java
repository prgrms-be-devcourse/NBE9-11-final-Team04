package com.team04.settlement.entity;

import com.team04.global.entity.BaseEntity;
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

    @Column
    private Long milestoneId;

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
    private Settlement(Long ideaId, Long milestoneId, SettlementType type,
                       Long totalAmount, Long platformFee, Long payoutAmount,
                       String idempotencyKey) {
        if (platformFee + payoutAmount != totalAmount) {
            throw new IllegalArgumentException("totalAmount는 platformFee + payoutAmount와 일치해야 합니다");
        }
        this.ideaId = ideaId;
        this.milestoneId = milestoneId;
        this.type = type;
        this.totalAmount = totalAmount;
        this.platformFee = platformFee;
        this.payoutAmount = payoutAmount;
        this.status = SettlementStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
    }
}