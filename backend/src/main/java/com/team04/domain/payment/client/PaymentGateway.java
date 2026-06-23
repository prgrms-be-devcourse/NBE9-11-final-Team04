package com.team04.domain.payment.client;

import com.team04.domain.payment.dto.response.PaymentConfirmResult;
import com.team04.domain.payment.dto.response.PaymentSessionResult;
import com.team04.domain.payment.dto.response.PaymentVerifyResult;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;

/** PG 연동 인터페이스 — 실제 토스 연동 전 Mock 구현체를 사용합니다. */
public interface PaymentGateway {

    PaymentSessionResult createSession(String orderId, long amount, PaymentMethod method);

    PaymentConfirmResult confirm(String paymentKey, String orderId, long amount);

    VirtualAccountIssueResult issueVirtualAccount(String orderId, long amount);

    /** 웹훅 수신 후 PG API로 입금·금액 재검증 (Read-through Verification) */
    PaymentVerifyResult verifyVirtualAccountDeposit(String orderId, long amount);

    /**
     * 선정산 지급 요청
     * 제안자 계좌로 실제 송금을 PG사에 요청합니다.
     * 실제 연동 시 토스페이먼츠 출금 API로 교체합니다.
     */
    void payout(Long preSettlementId, long amount);
}