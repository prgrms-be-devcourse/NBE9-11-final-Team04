package com.team04.domain.funding.event;

import com.team04.domain.funding.service.FundingService;
import com.team04.domain.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class FundingAchievementListener {

    private final FundingService fundingService;
    private final MilestoneService milestoneService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFundingPaid(FundingPaidEvent event) {
        fundingService.notifyAchievementUpdate(event);

        if (fundingService.isFundingGoalAchieved(event.ideaId())) {
            try {
                milestoneService.startFirstMilestone(event.ideaId());
                log.info("1단계 마일스톤 자동 시작 - ideaId: {}", event.ideaId());
            } catch (Exception e) {
                log.error("1단계 마일스톤 자동 시작 실패 - ideaId: {}, error: {}", event.ideaId(), e.getMessage());
            }
        }
    }
}
