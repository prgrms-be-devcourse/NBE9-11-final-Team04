package com.team04.domain.funding.event;

import com.team04.domain.funding.service.FundingService;
import com.team04.domain.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 후원 결제 완료 시 펀딩 달성률 SSE를 push합니다.
 * 아이디어 누적금 갱신은 Idea 도메인 {@link com.team04.domain.idea.event.IdeaFundingPaidListener}가 선행합니다.
 */

 * 후원 결제 완료 시 처리 리스너입니다.
 * 1. 펀딩 달성률 SSE push
 * 2. 펀딩 목표 달성 시 1단계 마일스톤 자동 시작
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

        // 펀딩 목표 달성 시 1단계 마일스톤 자동 시작
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