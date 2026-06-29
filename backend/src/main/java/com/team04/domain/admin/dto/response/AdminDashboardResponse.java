package com.team04.domain.admin.dto.response;

import com.team04.domain.dispute.dto.response.DisputeStatsResponse;
import com.team04.domain.user.dto.response.UserStatsResponse;

import java.util.Map;

public record AdminDashboardResponse(
        // 전체 프로젝트 현황
        Map<String, Long> ideaStats,

        // 승인 대기 / 진행 중 프로젝트 수
        long adminPendingCount,
        long inProgressCount,

        // 회원 현황
        UserStatsResponse userStats,

        // 분쟁/신고 처리 현황
        DisputeStatsResponse disputeStats,

        // 격리 전문가 계정 수
        long suspendedExpertCount
) {}