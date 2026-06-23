package com.team04.domain.payment.client;

import com.team04.domain.payment.dto.response.PaymentConfirmResult;
import com.team04.domain.payment.dto.response.PaymentRefundResult;
import com.team04.domain.payment.dto.response.PaymentSessionResult;
import com.team04.domain.payment.dto.response.PaymentVerifyResult;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 테스트용 Mock PG.
 * 토스 키 발급 전·테스트 프로필에서 카드 승인·가상계좌·환불을 흉내 냅니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payment.gateway.type", havingValue = "mock", matchIfMissing = true)
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

    @Override
    public PaymentVerifyResult verifyVirtualAccountDeposit(String orderId, long amount) {
        log.info("[MockPG] 가상계좌 입금 검증 orderId={}, amount={}", orderId, amount);
        if (orderId == null || orderId.isBlank() || amount <= 0) {
            return PaymentVerifyResult.failure("유효하지 않은 검증 요청");
        }
        return PaymentVerifyResult.success();
    }

    @Override
    public PaymentRefundResult refund(String paymentKey, String orderId, long amount, String cancelReason) {
        log.info("[MockPG] 환불 paymentKey={}, orderId={}, amount={}, reason={}",
                paymentKey, orderId, amount, cancelReason);
        if (paymentKey != null && paymentKey.startsWith("fail-refund-")) {
            return PaymentRefundResult.failure("Mock PG 환불 실패");
        }
        return PaymentRefundResult.success("mock-cancel-" + orderId);
    }
}
    /**
     * Mock 환경: 선정산 지급 요청 대체.
     * 실제 연동 시 토스페이먼츠 출금 API로 교체합니다.
     */
    @Override
    public void payout(Long preSettlementId, long amount) {
        log.info("[MockPG] 선정산 지급 요청 preSettlementId={}, amount={}", preSettlementId, amount);
    }
}
