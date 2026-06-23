package com.team04.domain.settlement.dto.response;

import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundReason;
import com.team04.domain.settlement.entity.RefundStatus;

import java.time.LocalDateTime;

public record RefundResponse(
        Long id,
        Long ideaId,
        Long paymentId,
        Long sponsorId,
        Long amount,
        RefundReason reason,
        RefundStatus status,
        String pgCancelKey,
        String failureReason,
        LocalDateTime createdAt
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getIdeaId(),
                refund.getPaymentId(),
                refund.getSponsorId(),
                refund.getAmount(),
                refund.getReason(),
                refund.getStatus(),
                refund.getPgCancelKey(),
                refund.getFailureReason(),
                refund.getCreatedAt()
        );
    }
}
