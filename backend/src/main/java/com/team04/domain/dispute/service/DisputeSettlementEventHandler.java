package com.team04.domain.dispute.service;

import com.team04.domain.settlement.service.RefundService;
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
    private final RefundService refundService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDisputeResolved(DisputeResolvedEvent event) {
        settlementService.createCancelRefundSettlement(event.ideaId());
        settlementService.createDepositForfeitSettlement(event.ideaId());
        refundService.createCancelRefunds(event.ideaId(), false);
    }
}
