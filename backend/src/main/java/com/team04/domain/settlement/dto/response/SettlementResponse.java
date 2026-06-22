package com.team04.domain.settlement.dto.response;

import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import com.team04.domain.settlement.entity.SettlementType;

import java.time.LocalDateTime;

public record SettlementResponse(
        Long settlementId,
        Long ideaId,
        SettlementType type,
        Long totalAmount,
        Long platformFee,
        Long payoutAmount,
        SettlementStatus status,
        LocalDateTime createdAt
) {
    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getIdeaId(),
                settlement.getType(),
                settlement.getTotalAmount(),
                settlement.getPlatformFee(),
                settlement.getPayoutAmount(),
                settlement.getStatus(),
                settlement.getCreatedAt()
        );
    }
}