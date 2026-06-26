package com.team04.infra.batch;

import com.team04.domain.payment.service.SettlementPaymentService;
import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 지급대행 실패 건을 주기적으로 다시 처리하는 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayoutRetryScheduler {

    private final SettlementPaymentService settlementPaymentService;

    /** 1분마다 FAILED 선정산/정산 지급 건을 재처리합니다. */
    @Scheduled(fixedDelayString = "${payment.payout.retry.scheduler-delay-ms:60000}")
    public void retryFailedPayouts() {
        log.debug("FAILED 지급대행 재처리 스케줄러 시작");
        retryFailedPreSettlements();
        retryFailedSettlements();
        log.debug("FAILED 지급대행 재처리 스케줄러 종료");
    }

    private void retryFailedPreSettlements() {
        for (PreSettlement preSettlement : settlementPaymentService.findFailedPreSettlements()) {
            try {
                // 3회 즉시 재시도 후에도 실패한 건은 다음 스케줄에서 다시 지급을 시도한다.
                settlementPaymentService.retryPreSettlementPayout(preSettlement.getId());
            } catch (Exception e) {
                log.error("선정산 지급 재처리 실패 preSettlementId={}, error={}",
                        preSettlement.getId(), e.getMessage(), e);
            }
        }
    }

    private void retryFailedSettlements() {
        for (Settlement settlement : settlementPaymentService.findFailedSettlements()) {
            try {
                // 정산 실패 건은 원래 완료되어야 할 상태를 복원한 뒤 다시 지급 흐름에 태운다.
                settlementPaymentService.retrySettlementPayout(
                        settlement.getId(),
                        resolveSuccessStatus(settlement)
                );
            } catch (Exception e) {
                log.error("정산 지급 재처리 실패 settlementId={}, error={}",
                        settlement.getId(), e.getMessage(), e);
            }
        }
    }

    private SettlementStatus resolveSuccessStatus(Settlement settlement) {
        // 현재 장부에는 지급 목적 컬럼이 없어 멱등성 키 suffix로 성공 상태를 복원한다.
        // 추후 payoutPurpose/retryCount 컬럼을 추가하면 이 분기는 리팩토링 대상이다.
        String key = settlement.getIdempotencyKey();
        if (key.contains("DEPOSIT-GOAL-NOT-MET") || key.contains("DEPOSIT-COMPLETED")) {
            return SettlementStatus.DEPOSIT_REFUNDED;
        }
        if (key.contains("DEPOSIT-REFUND")) {
            return SettlementStatus.PARTIALLY_REFUNDED;
        }
        return SettlementStatus.COMPLETED;
    }
}
