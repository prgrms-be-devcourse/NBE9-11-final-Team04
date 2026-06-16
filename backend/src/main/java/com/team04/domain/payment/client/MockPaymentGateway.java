package com.team04.domain.payment.client;

import com.team04.domain.payment.client.dto.PaymentConfirmResult;
import com.team04.domain.payment.client.dto.PaymentSessionResult;
import com.team04.domain.payment.client.dto.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 테스트용 Mock PG.
 * 토스 키 발급 전까지 카드 승인·가상계좌 발급을 흉내 냅니다.
 */
@Slf4j
@Component
public class MockPaymentGateway implements PaymentGateway {

    public static final String MOCK_CLIENT_KEY = "test_ck_mock_seedlink";

    private final AtomicLong virtualAccountSequence = new AtomicLong(1);

    @Override
    public PaymentSessionResult createSession(String orderId, long amount, PaymentMethod method) {
        log.info("[MockPG] 세션 생성 orderId={}, amount={}, method={}", orderId, amount, method);
        return new PaymentSessionResult(MOCK_CLIENT_KEY, "https://mock-pg.local/checkout?orderId=" + orderId);
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount) {
        log.info("[MockPG] 카드 승인 paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);
        if (paymentKey != null && paymentKey.startsWith("fail-")) {
            return PaymentConfirmResult.failure("Mock PG 승인 실패");
        }
        String resolvedKey = paymentKey != null ? paymentKey : "mock-pg-" + orderId;
        return PaymentConfirmResult.success(resolvedKey);
    }

    @Override
    public VirtualAccountIssueResult issueVirtualAccount(String orderId, long amount) {
        log.info("[MockPG] 가상계좌 발급 orderId={}, amount={}", orderId, amount);
        long id = virtualAccountSequence.getAndIncrement();
        return new VirtualAccountIssueResult(
                id,
                "088",
                String.format("%012d", id),
                LocalDateTime.now().plusDays(3)
        );
    }
}
