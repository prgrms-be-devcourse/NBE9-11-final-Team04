package com.team04.domain.settlement.dto.response;

import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.PreSettlementStatus;

import java.time.LocalDateTime;

/** 선정산 응답 DTO입니다. */
public record PreSettlementResponse(
        Long preSettlementId,
        Long milestoneId,
        Long ideaId,
        Long amount,
        PreSettlementStatus status,
        LocalDateTime requestedAt
) {
    public static PreSettlementResponse from(PreSettlement preSettlement) {
        return new PreSettlementResponse(
                preSettlement.getId(),
                preSettlement.getMilestoneId(),
                preSettlement.getIdeaId(),
                preSettlement.getAmount(),
                preSettlement.getStatus(),
                preSettlement.getRequestedAt()
        );
    }
}