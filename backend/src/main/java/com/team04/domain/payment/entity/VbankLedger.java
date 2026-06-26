package com.team04.domain.payment.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
// 멱등키 unique 제약으로 중복 장부를 막고, 아이디어별 최신순 조회를 위해 (idea_id, id) 인덱스를 둔다.
@Table(
        name = "vbank_ledgers",
        uniqueConstraints = @UniqueConstraint(name = "uk_vbank_ledgers_idempotency_key", columnNames = "idempotency_key"),
        indexes = @Index(name = "idx_vbank_ledgers_idea_id_id", columnList = "idea_id, id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VbankLedger extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idea_id", nullable = false)
    private Long ideaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VbankLedgerType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VbankLedgerDirection direction;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "affects_balance", nullable = false)
    private Boolean affectsBalance;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "reference_type", length = 64)
    private String referenceType;

    @Column(name = "reference_id")
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
