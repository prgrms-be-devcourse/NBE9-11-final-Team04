package com.team04.infra.batch;

import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.service.MilestoneService;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/** 정산 관련 스케줄링 작업을 처리하는 컴포넌트입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final IdeaService ideaService;
    private final MilestoneService milestoneService;
    private final SettlementService settlementService;
    private final RefundService refundService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 매일 자정 실행
     * 펀딩 마감된 프로젝트를 성공/실패로 확정합니다.
     * 성공 프로젝트는 1단계 마일스톤을 시작하고, 실패 프로젝트는
     * 환불 장부(Settlement) + 후원자별 환불 레코드(Refund)를 자동 생성합니다.
     * TransactionTemplate으로 프로젝트별 단일 트랜잭션 보장 —
     * Settlement 생성 후 Refund 생성 실패 시 전체 롤백됩니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processClosedFundings() {
        log.info("펀딩 마감 확정 스케줄러 시작");

        List<Long> successfulIdeaIds = ideaService.getSuccessfulFundingIdeaIds();
        for (Long ideaId : successfulIdeaIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    ideaService.startFundingIfOpen(ideaId);
                    milestoneService.startFirstMilestone(ideaId);
                });
                log.info("펀딩 성공 마일스톤 시작 완료 - ideaId: {}", ideaId);
            } catch (Exception e) {
                log.error("펀딩 성공 마일스톤 시작 실패 - ideaId: {}, error: {}", ideaId, e.getMessage());
            }
        }

        List<Long> failedIdeaIds = ideaService.getFailedFundingIdeaIds();

        for (Long ideaId : failedIdeaIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    settlementService.createGoalNotMetRefundSettlement(ideaId);
                    settlementService.createGoalNotMetDepositRefundSettlement(ideaId);
                    refundService.createGoalNotMetRefunds(ideaId);
                    ideaService.cancelIdea(ideaId);
                });
                log.info("환불 처리 완료 - ideaId: {}", ideaId);
            } catch (Exception e) {
                log.error("환불 처리 실패 - ideaId: {}, error: {}", ideaId, e.getMessage());
            }
        }

        log.info("펀딩 마감 확정 스케줄러 종료");
    }
}
