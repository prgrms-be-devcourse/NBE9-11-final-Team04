package com.team04.domain.funding.event;

import com.team04.domain.funding.service.FundingService;
import com.team04.domain.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 후원 결제 완료({@link FundingPaidEvent}) 후처리 리스너입니다.
 *
 * <p>실행 순서: {@link com.team04.domain.idea.event.IdeaFundingPaidListener} {@code @Order(1)}
 * → 본 리스너 {@code @Order(2)}
 *
 * <p>담당 업무:
 * <ul>
 *   <li>펀딩 달성률 SSE push</li>
 *   <li>목표 달성 시 1단계 마일스톤 자동 시작</li>
 * </ul>
 */
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
