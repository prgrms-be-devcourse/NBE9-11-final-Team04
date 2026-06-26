package com.team04.domain.funding.event;

import com.team04.domain.funding.service.FundingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Order(2)
@RequiredArgsConstructor
public class FundingAchievementListener {

    private final FundingService fundingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFundingPaid(FundingPaidEvent event) {
        fundingService.notifyAchievementUpdate(event);
    }
}
