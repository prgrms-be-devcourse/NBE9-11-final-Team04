package com.team04.domain.idea.event;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.event.FundingAchievementListener;
import com.team04.domain.funding.event.FundingPaidEvent;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.milestone.service.MilestoneService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 후원 결제 완료({@link FundingPaidEvent}) 시 아이디어 집계 필드를 갱신합니다.
 *
 * <p>Payment/Funding 도메인은 결제·후원 상태만 담당하고,
 * 아이디어의 {@code currentAmount}·{@code sponsorCount} 반영은 Idea 도메인 책임입니다.
 *
 * <p>{@link Funding#markAmountAppliedToIdea()} 플래그로 웹훅·이벤트 중복 수신 시
 * 누적 후원금이 두 번 올라가지 않도록 멱등 처리합니다.
 *
 * <p>실행 순서: 본 리스너 {@code @Order(1)} (집계·목표 달성 시 1단계 마일스톤 시작)
 * → {@link FundingAchievementListener} {@code @Order(2)} (SSE)
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class IdeaFundingPaidListener {

    private final IdeaRepository ideaRepository;
    private final FundingRepository fundingRepository;
    private final MilestoneService milestoneService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFundingPaid(FundingPaidEvent event) {
        Funding funding = fundingRepository.findByIdForUpdate(event.fundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        // 동일 Funding에 대한 FundingPaidEvent 재전송 시 스킵
        if (funding.isAmountAppliedToIdea()) {
            return;
        }

        Idea idea = ideaRepository.findByIdForUpdate(event.ideaId())
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        idea.addFundingAmount(event.amount());
        funding.markAmountAppliedToIdea();

        if (idea.getCurrentAmount() >= idea.getGoalAmount()) {
            milestoneService.startFirstMilestone(event.ideaId());
        }
    }
}
