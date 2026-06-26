package com.team04.domain.dispute.service;

import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.event.DisputeResolvedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DisputeSettlementEventHandler {

    private final SettlementService settlementService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDisputeResolved(DisputeResolvedEvent event) {
        // 분쟁 해결로 프로젝트 환불이 확정되면 정산 장부와 환불 장부를 하나의 흐름으로 생성한다.
        settlementService.forceRefund(event.ideaId());
    }
}
