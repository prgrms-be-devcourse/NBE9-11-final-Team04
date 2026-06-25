package com.team04.infra.batch;

import com.team04.domain.payment.service.SettlementPaymentService;
import com.team04.domain.settlement.entity.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Refund(PENDING) 건을 PG 환불 API로 처리하는 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundPendingScheduler {

    private final SettlementPaymentService settlementPaymentService;

    /** 1분마다 PENDING 환불 건을 순회하며 PG 환불 + Payment 동기화를 수행합니다. */
    @Scheduled(fixedDelayString = "${payment.refund-pending.delay-ms:60000}")
    public void processPendingRefunds() {
        log.debug("PENDING 환불 처리 스케줄러 시작");
        for (Refund refund : settlementPaymentService.findPendingRefunds()) {
            try {
                settlementPaymentService.processRefund(refund.getId());
            } catch (Exception e) {
                log.error("환불 처리 실패 refundId={}, error={}", refund.getId(), e.getMessage(), e);
            }
        }
        log.debug("PENDING 환불 처리 스케줄러 종료");
    }
}
