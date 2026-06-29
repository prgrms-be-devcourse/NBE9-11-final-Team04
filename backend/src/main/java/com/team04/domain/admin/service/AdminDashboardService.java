package com.team04.domain.admin.service;

import com.team04.domain.admin.dto.response.AdminDashboardResponse;
import com.team04.domain.dispute.service.DisputeService;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.service.IdeaAdminService;
import com.team04.domain.user.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final IdeaAdminService ideaAdminService;
    private final AdminUserService adminUserService;
    private final DisputeService disputeService;
    private final ExpertProfileRepository expertProfileRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        // 전체 프로젝트 현황 — 상태별 집계
        Map<IdeaStatus, Long> ideaStatusMap = ideaAdminService.getStatusStats();
        Map<String, Long> ideaStats = ideaStatusMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));

        // 승인 대기 / 진행 중 프로젝트 수
        long adminPendingCount = ideaStatusMap.getOrDefault(IdeaStatus.ADMIN_PENDING, 0L);
        long inProgressCount = ideaStatusMap.getOrDefault(IdeaStatus.IN_PROGRESS, 0L);

        // 회원 현황
        var userStats = adminUserService.getUserStats();

        // 분쟁/신고 처리 현황
        var disputeStats = disputeService.getDisputeStats();

        // 격리 전문가 계정 수
        long suspendedExpertCount = expertProfileRepository
                .findProfilesByStatus(ExpertStatus.SUSPENDED, PageRequest.of(0, 1))
                .getTotalElements();

        return new AdminDashboardResponse(
                ideaStats,
                adminPendingCount,
                inProgressCount,
                userStats,
                disputeStats,
                suspendedExpertCount
        );
    }
}