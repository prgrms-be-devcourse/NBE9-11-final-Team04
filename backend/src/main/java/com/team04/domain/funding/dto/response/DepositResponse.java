package com.team04.domain.funding.dto.response;

import com.team04.domain.funding.entity.Deposit;

import java.time.LocalDateTime;

public record DepositResponse(
        Long depositId,
        Long ideaId,
        Long userId,
        Long amount,
        String status,
        LocalDateTime paidAt,
        LocalDateTime releasedAt
) {

    public static DepositResponse from(Deposit deposit) {
        return new DepositResponse(
                deposit.getId(),
                deposit.getIdeaId(),
                deposit.getUserId(),
                deposit.getAmount(),
                deposit.getStatus().name(),
                deposit.getPaidAt(),
                deposit.getReleasedAt()
        );
    }
}
