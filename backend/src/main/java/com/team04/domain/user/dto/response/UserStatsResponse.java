package com.team04.domain.user.dto.response;

public record UserStatsResponse(
        long totalCount,
        long proposerCount,
        long expertCount,
        long sponsorCount,
        long suspendedCount,
        long withdrawnCount
) {}