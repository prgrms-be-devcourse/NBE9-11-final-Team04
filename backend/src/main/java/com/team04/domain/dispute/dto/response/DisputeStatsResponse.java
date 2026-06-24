package com.team04.domain.dispute.dto.response;

public record DisputeStatsResponse(
        long totalCount,
        long receivedCount,
        long pendingCount,
        long resolvedCount,
        long rejectedCount
) {
}
