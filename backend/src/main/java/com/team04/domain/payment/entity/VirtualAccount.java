package com.team04.domain.payment.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VirtualAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String bankCode;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private LocalDateTime dueDate;

    @Column(nullable = false)
    private Long amount;

    public static VirtualAccount create(String orderId, String bankCode, String accountNumber,
                                        LocalDateTime dueDate, Long amount) {
        VirtualAccount account = new VirtualAccount();
        account.orderId = orderId;
        account.bankCode = bankCode;
        account.accountNumber = accountNumber;
        account.dueDate = dueDate;
        account.amount = amount;
        return account;
    }
}
