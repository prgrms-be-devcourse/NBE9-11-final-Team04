package com.team04.domain.funding.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "deposits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deposit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FundingTypes.DepositStatus status;

    private LocalDateTime paidAt;

    private LocalDateTime releasedAt;

    /** 보증금 PG 결제와 연결 (결제 완료 후 HELD 확정) */
    private Long paymentId;

    public static Deposit createHeld(Long ideaId, Long userId, Long amount) {
        return createHeld(ideaId, userId, amount, null);
    }

    public static Deposit createHeld(Long ideaId, Long userId, Long amount, Long paymentId) {
        Deposit deposit = new Deposit();
        deposit.ideaId = ideaId;
        deposit.userId = userId;
        deposit.amount = amount;
        deposit.paymentId = paymentId;
        deposit.status = FundingTypes.DepositStatus.HELD;
        deposit.paidAt = LocalDateTime.now();
        return deposit;
    }

    public void release() {
        if (this.status == FundingTypes.DepositStatus.REFUNDED) {
            return;
        }
        if (this.status != FundingTypes.DepositStatus.HELD) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.status = FundingTypes.DepositStatus.REFUNDED;
        this.releasedAt = LocalDateTime.now();
    }

    public void forfeit() {
        if (this.status == FundingTypes.DepositStatus.FORFEITED) {
            return;
        }
        if (this.status != FundingTypes.DepositStatus.HELD) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        this.status = FundingTypes.DepositStatus.FORFEITED;
        this.releasedAt = LocalDateTime.now();
    }
}
