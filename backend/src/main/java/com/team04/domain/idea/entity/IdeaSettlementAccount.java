package com.team04.domain.idea.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 아이디어 제안자의 정산 및 환불 입금 계좌 정보를 저장하는 엔티티입니다. */
@Entity
@Table(
        name = "idea_settlement_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_idea_settlement_account_idea_id", columnNames = "idea_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdeaSettlementAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idea_id", nullable = false)
    private Long ideaId;

    @Column(nullable = false, length = 50)
    private String bankName;

    @Column(nullable = false, length = 30)
    private String accountNumber;

    @Column(nullable = false, length = 50)
    private String accountHolderName;

    /** 신규 계좌 정보를 생성합니다. */
    public static IdeaSettlementAccount create(
            Long ideaId,
            String bankName,
            String accountNumber,
            String accountHolderName
    ) {
        IdeaSettlementAccount account = new IdeaSettlementAccount();
        account.ideaId = ideaId;
        account.update(bankName, accountNumber, accountHolderName);
        return account;
    }

    /** 제안자가 관리자 최종 승인 전에 입력한 계좌 정보를 최신 값으로 갱신합니다. */
    public void update(String bankName, String accountNumber, String accountHolderName) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
    }
}