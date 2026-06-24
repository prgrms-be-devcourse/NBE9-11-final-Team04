package com.team04.domain.payment.dto.request;

/** 선정산(정산) 지급대행 요청 */
public record PayoutRequest(
        Long preSettlementId,
        Long ideaId,
        long amount,
        String accountHolder,
        String bankCode,
        String accountNumber
) {
}
