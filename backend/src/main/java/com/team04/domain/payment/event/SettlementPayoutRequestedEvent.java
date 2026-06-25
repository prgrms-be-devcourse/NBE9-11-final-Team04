package com.team04.domain.payment.event;

import com.team04.domain.settlement.entity.SettlementStatus;

/** 최종 정산/보증금 환급 장부 저장·커밋 후 PG 지급 처리를 요청하는 이벤트입니다. */
public record SettlementPayoutRequestedEvent(
        Long settlementId,
        SettlementStatus successStatus
) {
}
