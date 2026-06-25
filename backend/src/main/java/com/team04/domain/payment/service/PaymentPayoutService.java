package com.team04.domain.payment.service;

import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.dto.request.PayoutRequest;
import com.team04.domain.payment.dto.response.PayoutResult;
import com.team04.domain.payment.exception.PayoutRetryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPayoutService {

    private final PaymentGateway paymentGateway;

    @Value("${payment.payout.retry.max-attempts:3}")
    private int maxPayoutAttempts;

    /**
     * 지급대행 실패는 일시적 외부 오류일 수 있어 Spring Retry로 최대 3회 재시도한다.
     * skipped 응답은 실제 지급 생략을 뜻하지만 성공 응답이므로 재시도하지 않는다.
     */
    @Retryable(
            retryFor = PayoutRetryException.class,
            maxAttemptsExpression = "${payment.payout.retry.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${payment.payout.retry.delay-ms:1000}")
    )
    public PayoutResult payout(PayoutRequest request) {
        try {
            PayoutResult result = paymentGateway.payout(request);
            if (!result.success()) {
                // 실패 응답도 예외로 바꿔야 Spring Retry가 동일한 방식으로 재시도한다.
                throw new PayoutRetryException(result.failureMessage());
            }
            return result;
        } catch (PayoutRetryException exception) {
            throw exception;
        } catch (Exception exception) {
            // PG 클라이언트 예외도 지급 실패로 통일해 recover()에서 최종 실패 응답으로 돌려준다.
            throw new PayoutRetryException(exception.getMessage(), exception);
        }
    }

    @Recover
    public PayoutResult recover(PayoutRetryException exception, PayoutRequest request) {
        // 3회 모두 실패하면 상위 서비스가 기존 실패 상태 전환 로직을 탈 수 있도록 failure 결과를 반환한다.
        log.error("지급대행 {}회 재시도 실패 type={}, targetId={}, amount={}, message={}",
                maxPayoutAttempts,
                request.payoutTargetType(),
                request.payoutTargetId(),
                request.amount(),
                exception.getMessage(),
                exception);
        return PayoutResult.failure(exception.getMessage());
    }
}
