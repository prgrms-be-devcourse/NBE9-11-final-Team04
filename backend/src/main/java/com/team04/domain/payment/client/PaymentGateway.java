package com.team04.domain.payment.client;

import com.team04.domain.payment.dto.request.PayoutRequest;
import com.team04.domain.payment.dto.response.PaymentConfirmResult;
import com.team04.domain.payment.dto.response.PaymentRefundResult;
import com.team04.domain.payment.dto.response.PaymentSessionResult;
import com.team04.domain.payment.dto.response.PaymentVerifyResult;
import com.team04.domain.payment.dto.response.PayoutResult;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;

/** PG 연동 인터페이스 */
public interface PaymentGateway {

    PaymentSessionResult createSession(String orderId, long amount, PaymentMethod method);

    PaymentConfirmResult confirm(String paymentKey, String orderId, long amount);

    VirtualAccountIssueResult issueVirtualAccount(String orderId, long amount);

    /** 웹훅 수신 후 PG API로 입금·금액 재검증 */
    PaymentVerifyResult verifyVirtualAccountDeposit(String orderId, long amount);

    /** PG 결제 취소(환불) API */
    PaymentRefundResult refund(String paymentKey, String orderId, long amount, String cancelReason);

    /**
     * 선정산 지급 요청 (dev 호환).
     * 정산 도메인 PR에서 {@link #payout(PayoutRequest)}로 확장합니다.
     */
    void payout(Long preSettlementId, long amount);

    /** 확장 지급대행 — 계좌 정보 포함. 기본 구현은 {@link #payout(Long, long)}에 위임합니다. */
    default PayoutResult payout(PayoutRequest request) {
        payout(request.preSettlementId(), request.amount());
        return PayoutResult.success("payout-" + request.preSettlementId());
    }

    /** true면 create 시 가상계좌를 즉시 발급(Mock). false면 confirm 이후 발급(Toss). */
    default boolean issuesVirtualAccountAtCreateTime() {
        return true;
    }
}
