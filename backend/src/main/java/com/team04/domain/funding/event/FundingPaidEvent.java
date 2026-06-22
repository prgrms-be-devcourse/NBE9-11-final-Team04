package com.team04.domain.funding.event;

/**
 * 후원 결제가 완료되어 Funding이 PAID가 되었을 때 발행됩니다.
 * Idea 도메인에서 {@code @TransactionalEventListener}로 수신해 currentAmount를 갱신합니다.
 */
public record FundingPaidEvent(
        Long fundingId,
        Long ideaId,
        Long sponsorId,
        Long amount
) {
}
