package com.team04.infra.batch;

import com.team04.domain.idea.service.IdeaService;
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
    private final SettlementService settlementService;
    private final RefundService refundService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 매일 자정 실행
     * 펀딩 마감됐고 목표 금액 미달성인 프로젝트를 감지해
     * 환불 장부(Settlement PENDING) + 후원자별 Refund(PENDING) 생성 후 PG 환불을 실행합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processFailedFundingRefunds() {
        log.info("목표 미달성 자동 환불 스케줄러 시작");

        List<Long> failedIdeaIds = ideaService.getFailedFundingIdeaIds();

        for (Long ideaId : failedIdeaIds) {
            try {
                List<Long> refundIds = transactionTemplate.execute(status -> {
                    settlementService.createGoalNotMetRefundSettlement(ideaId);
                    return refundService.createGoalNotMetRefunds(ideaId);
                });

                if (refundIds != null) {
                    for (Long refundId : refundIds) {
                        try {
                            refundService.executeRefund(refundId);
                        } catch (Exception e) {
                            log.error("개별 환불 PG 실행 실패 - refundId: {}, error: {}", refundId, e.getMessage());
                        }
                    }
                    refundService.tryFinalizeRefundSettlement(ideaId);
                }
                log.info("환불 처리 완료 - ideaId: {}", ideaId);
            } catch (Exception e) {
                log.error("환불 처리 실패 - ideaId: {}, error: {}", ideaId, e.getMessage());
            }
        }

        log.info("목표 미달성 자동 환불 스케줄러 종료");
    }
}
