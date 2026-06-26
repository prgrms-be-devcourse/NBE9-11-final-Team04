package com.team04.domain.idea.dto.response;

import com.team04.domain.idea.entity.IdeaSettlementAccount;

/** 아이디어 제안자의 정산 및 환불 계좌 조회 응답 DTO입니다. */
public record IdeaSettlementAccountResponse(
        Long ideaId,
        String bankName,
        String accountNumber,
        String accountHolderName
) {

    /**
     * 엔티티의 계좌 정보를 API 응답 형식으로 변환합니다.
     */
    public static IdeaSettlementAccountResponse of(IdeaSettlementAccount account) {
        return new IdeaSettlementAccountResponse(
                account.getIdeaId(),
                account.getBankName(),
                account.getAccountNumber(),
                account.getAccountHolderName()
        );
    }
}