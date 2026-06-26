package com.team04.domain.payment.dto.response;

import com.team04.domain.payment.entity.VbankLedger;
import com.team04.domain.payment.entity.VbankLedgerDirection;
import com.team04.domain.payment.entity.VbankLedgerType;

import java.time.LocalDateTime;

public record VbankLedgerResponse(
        Long ledgerId,
        Long ideaId,
        VbankLedgerType type,
        VbankLedgerDirection direction,
        Long amount,
        Long balanceAfter,
        Boolean affectsBalance,
        String referenceType,
        Long referenceId,
        String memo,
        LocalDateTime createdAt
) {
    public static VbankLedgerResponse from(VbankLedger ledger) {
        return new VbankLedgerResponse(
                ledger.getId(),
                ledger.getIdeaId(),
                ledger.getType(),
                ledger.getDirection(),
                ledger.getAmount(),
                ledger.getBalanceAfter(),
                ledger.getAffectsBalance(),
                ledger.getReferenceType(),
                ledger.getReferenceId(),
                ledger.getMemo(),
                ledger.getCreatedAt()
        );
    }
}
