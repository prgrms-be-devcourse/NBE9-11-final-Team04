package com.team04.domain.payment.entity;

import com.team04.global.entity.BaseEntity;
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

@Entity
@Table(name = "vbank_ledgers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VbankLedger extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VbankLedgerType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VbankLedgerDirection direction;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private Boolean affectsBalance;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(length = 64)
    private String referenceType;

    private Long referenceId;

    @Column(columnDefinition = "TEXT")
    private String memo;

    public static VbankLedger create(
            Long ideaId,
            VbankLedgerType type,
            VbankLedgerDirection direction,
            Long amount,
            Long balanceAfter,
            boolean affectsBalance,
            String idempotencyKey,
            String referenceType,
            Long referenceId,
            String memo
    ) {
        if (ideaId == null) {
            throw new IllegalArgumentException("아이디어 ID는 필수입니다");
        }
        if (type == null) {
            throw new IllegalArgumentException("장부 유형은 필수입니다");
        }
        if (direction == null) {
            throw new IllegalArgumentException("입출금 방향은 필수입니다");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키는 필수입니다");
        }
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("장부 금액은 0 이상이어야 합니다");
        }
        if (balanceAfter == null || balanceAfter < 0) {
            throw new IllegalArgumentException("장부 잔액은 0 이상이어야 합니다");
        }

        VbankLedger ledger = new VbankLedger();
        ledger.ideaId = ideaId;
        ledger.type = type;
        ledger.direction = direction;
        ledger.amount = amount;
        ledger.balanceAfter = balanceAfter;
        ledger.affectsBalance = affectsBalance;
        ledger.idempotencyKey = idempotencyKey;
        ledger.referenceType = referenceType;
        ledger.referenceId = referenceId;
        ledger.memo = memo;
        return ledger;
    }
}
