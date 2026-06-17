package com.team04.domain.funding.dto.response;

import java.time.LocalDateTime;

public record FundingResponse(
        Long fundingId,
        Long ideaId,
        Long sponsorId,
        Long amount,
        String status,
        LocalDateTime createdAt
) {
}
