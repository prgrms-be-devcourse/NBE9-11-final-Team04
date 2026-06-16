package com.team04.domain.payment.client;

import com.team04.domain.payment.client.dto.PaymentConfirmResult;
import com.team04.domain.payment.client.dto.PaymentSessionResult;
import com.team04.domain.payment.client.dto.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;

/** PG 연동 인터페이스 — 실제 토스 연동 전 Mock 구현체를 사용합니다. */
public interface PaymentGateway {

    PaymentSessionResult createSession(String orderId, long amount, PaymentMethod method);

    PaymentConfirmResult confirm(String paymentKey, String orderId, long amount);

    VirtualAccountIssueResult issueVirtualAccount(String orderId, long amount);
}
