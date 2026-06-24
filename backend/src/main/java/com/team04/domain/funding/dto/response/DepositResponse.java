package com.team04.domain.funding.dto.response;

import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.payment.dto.response.PaymentResponse;

import java.time.LocalDateTime;

public record DepositResponse(
        Long depositId,
        Long ideaId,
        Long userId,
        Long amount,
        String status,
        LocalDateTime paidAt,
        LocalDateTime releasedAt,
        /** 보증금 PG 결제 세션 — 결제 완료 전에는 depositId가 null일 수 있습니다 */
        PaymentResponse payment
) {

    public static DepositResponse from(Deposit deposit) {
        return from(deposit, null);
    }

    public static DepositResponse from(Deposit deposit, PaymentResponse payment) {
        return new DepositResponse(
                deposit.getId(),
                deposit.getIdeaId(),
                deposit.getUserId(),
                deposit.getAmount(),
                deposit.getStatus().name(),
                deposit.getPaidAt(),
                deposit.getReleasedAt(),
                payment
        );
    }

    /** PG 결제 대기 중 — 보증금 레코드는 결제 성공 후 생성됩니다 */
    public static DepositResponse pendingPayment(Long ideaId, Long userId, Long amount, PaymentResponse payment) {
        return new DepositResponse(
                null,
                ideaId,
                userId,
                amount,
                "PENDING_PAYMENT",
                null,
                null,
                payment
        );
    }
}
