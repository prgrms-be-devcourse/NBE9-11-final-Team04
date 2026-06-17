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

import java.time.LocalDateTime;

@Entity
@Table(name = "vbank_deposits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VbankDeposit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long paymentId;

    /** workspace.virtual_account FK — 엔티티는 워크스페이스에서 관리 */
    @Column(nullable = false)
    private Long virtualAccountId;

    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentTypes.VbankDepositStatus depositStatus;

    private LocalDateTime depositedAt;

    public static VbankDeposit createWaiting(
            Long paymentId,
            Long virtualAccountId,
            String bankCode,
            String accountNumber,
            LocalDateTime dueDate
    ) {
        VbankDeposit deposit = new VbankDeposit();
        deposit.paymentId = paymentId;
        deposit.virtualAccountId = virtualAccountId;
        deposit.bankCode = bankCode;
        deposit.accountNumber = accountNumber;
        deposit.dueDate = dueDate;
        deposit.depositStatus = PaymentTypes.VbankDepositStatus.WAITING;
        return deposit;
    }

    public void markDeposited() {
        this.depositStatus = PaymentTypes.VbankDepositStatus.DONE;
        this.depositedAt = LocalDateTime.now();
    }
}
