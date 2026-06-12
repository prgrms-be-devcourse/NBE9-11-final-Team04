package com.team04.settlement.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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
    private SettlementTypes.SettlementType type;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private Long platformFee;

    @Column(nullable = false)
    private Long payoutAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementTypes.SettlementStatus status;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;
}