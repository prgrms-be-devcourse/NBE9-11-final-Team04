package com.team04.domain.user.dto.response;

public record UserStatsResponse(
        long totalCount,
        long userCount,
        long expertCount,
        long suspendedCount,
        long withdrawnCount
) {}