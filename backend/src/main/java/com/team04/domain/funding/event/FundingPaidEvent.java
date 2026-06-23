package com.team04.domain.funding.event;

/**
 * 후원 결제가 완료되어 Funding이 PAID가 되었을 때 발행됩니다.
 * Idea 도메인의 {@link com.team04.domain.idea.event.IdeaFundingPaidListener}가 수신해 currentAmount를 갱신합니다.
 */
public record FundingPaidEvent(
        Long fundingId,
        Long ideaId,
        Long sponsorId,
        Long amount
) {
}
