package com.team04.domain.payment.dto.response;

import java.time.LocalDateTime;

public record VirtualAccountIssueResult(
        Long virtualAccountId,
        String bankCode,
        String accountNumber,
        LocalDateTime dueDate
) {
}
