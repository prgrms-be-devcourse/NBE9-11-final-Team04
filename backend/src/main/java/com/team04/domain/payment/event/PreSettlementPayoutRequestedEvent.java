package com.team04.domain.payment.event;

/** 선정산 저장·커밋 후 PG 지급 처리를 요청하는 이벤트입니다. */
public record PreSettlementPayoutRequestedEvent(Long preSettlementId) {
}
