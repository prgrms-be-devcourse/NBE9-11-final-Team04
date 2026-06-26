package com.team04.domain.payment.dto.request;

/** 선정산, 최종 정산, 보증금 환급에 공통으로 사용하는 지급대행 요청입니다. */
public record PayoutRequest(
        Long payoutTargetId,
        PayoutTargetType payoutTargetType,
        Long ideaId,
        long amount,
        String accountHolder,
        String bankCode,
        String accountNumber
) {
    public static PayoutRequest preSettlement(
            Long preSettlementId,
            Long ideaId,
            long amount,
            String accountHolder,
            String bankCode,
            String accountNumber
    ) {
        return new PayoutRequest(
                preSettlementId,
                PayoutTargetType.PRE_SETTLEMENT,
                ideaId,
                amount,
                accountHolder,
                bankCode,
                accountNumber
        );
    }

    public static PayoutRequest settlement(
            Long settlementId,
            Long ideaId,
            long amount,
            String accountHolder,
            String bankCode,
            String accountNumber
    ) {
        return new PayoutRequest(
                settlementId,
                PayoutTargetType.SETTLEMENT,
                ideaId,
                amount,
                accountHolder,
                bankCode,
                accountNumber
        );
    }
}
