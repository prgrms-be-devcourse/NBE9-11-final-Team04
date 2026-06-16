package com.team04.domain.payment.client.dto;

import java.time.LocalDateTime;

public record VirtualAccountIssueResult(
        Long virtualAccountId,
        String bankCode,
        String accountNumber,
        LocalDateTime dueDate
) {
}
