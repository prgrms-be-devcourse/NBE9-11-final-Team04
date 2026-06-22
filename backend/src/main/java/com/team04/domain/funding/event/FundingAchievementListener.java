package com.team04.domain.funding.event;

import com.team04.domain.funding.service.FundingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 후원 결제 완료 시 펀딩 달성률 SSE를 push합니다.
 */
@Component
@RequiredArgsConstructor
public class FundingAchievementListener {

    private final FundingService fundingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFundingPaid(FundingPaidEvent event) {
        fundingService.notifyAchievementUpdate(event);
    }
}
