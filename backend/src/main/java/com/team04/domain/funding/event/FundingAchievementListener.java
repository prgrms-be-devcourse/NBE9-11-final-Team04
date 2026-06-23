package com.team04.domain.funding.event;

import com.team04.domain.funding.service.FundingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 후원 결제 완료 시 펀딩 달성률 SSE를 push합니다.
 * 아이디어 누적금 갱신은 Idea 도메인 {@link com.team04.domain.idea.event.IdeaFundingPaidListener}가 선행합니다.
 */

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
