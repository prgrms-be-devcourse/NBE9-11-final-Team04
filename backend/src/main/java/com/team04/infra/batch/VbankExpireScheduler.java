package com.team04.infra.batch;

import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.VbankDepositStatus;
import com.team04.domain.payment.entity.VbankDeposit;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.payment.repository.VbankDepositRepository;
import com.team04.domain.payment.service.PaymentTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VbankExpireScheduler {

    private final VbankDepositRepository vbankDepositRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTxService paymentTxService;

    /** 매일 새벽 1시 — 입금 기한이 지난 가상계좌를 EXPIRED 처리 */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expireOverdueVirtualAccounts() {
        log.info("가상계좌 만료 스케줄러 시작");

        List<VbankDeposit> overdue = vbankDepositRepository.findByDepositStatusAndDueDateBefore(
                VbankDepositStatus.WAITING,
                LocalDateTime.now()
        );

        for (VbankDeposit vbankDeposit : overdue) {
            try {
                vbankDeposit.markExpired();
                Payment payment = paymentRepository.findById(vbankDeposit.getPaymentId()).orElse(null);
                if (payment != null) {
                    paymentTxService.failPayment(payment.getId());
                }
                log.info("가상계좌 만료 처리 - paymentId: {}", vbankDeposit.getPaymentId());
            } catch (Exception e) {
                log.error("가상계좌 만료 처리 실패 - paymentId: {}, error: {}",
                        vbankDeposit.getPaymentId(), e.getMessage());
            }
        }

        log.info("가상계좌 만료 스케줄러 종료 - 처리 건수: {}", overdue.size());
    }
}
