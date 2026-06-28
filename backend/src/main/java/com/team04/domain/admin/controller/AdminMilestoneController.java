package com.team04.domain.admin.controller;

import com.team04.domain.milestone.dto.request.RejectReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.service.AdminMilestoneService;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/milestones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMilestoneController {

    private final AdminMilestoneService adminMilestoneService;

    /** 관리자 검토 대기 마일스톤 목록 조회 API */
    @GetMapping("/pending-reports")
    public ApiResponse<List<MilestoneResponse>> getPendingReportMilestones() {
        return ApiResponse.ofSuccess(adminMilestoneService.getPendingReportMilestones());
    }

    /** 완료 보고서 승인 API */
    @PostMapping("/{milestoneId}/reports/approve/completion")
    public ApiResponse<CompletionReportResponse> approveCompletionReport(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(adminMilestoneService.approveCompletionReport(milestoneId));
    }

    /** 소명 보고서 승인 API */
    @PostMapping("/{milestoneId}/reports/approve/appeal")
    public ApiResponse<CompletionReportResponse> approveAppealReport(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(adminMilestoneService.approveAppealReport(milestoneId));
    }

    /** 완료/소명 보고서 반려 API */
    @PostMapping("/{milestoneId}/reports/reject")
    public ApiResponse<CompletionReportResponse> rejectReport(
            @PathVariable Long milestoneId,
            @Valid @RequestBody RejectReportRequest request
    ) {
        return ApiResponse.ofSuccess(adminMilestoneService.rejectReport(milestoneId, request));
    }

    /**
     * 소명 중단 인정 + 환불 처리 API
     * 관리자가 제안자의 중단 소명을 인정할 때 사용합니다.
     */
    @PostMapping("/{milestoneId}/reports/refund")
    public ApiResponse<Void> refundMilestone(@PathVariable Long milestoneId) {
        adminMilestoneService.refundMilestone(milestoneId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 마일스톤 이행 중단 처리 API */
    @PostMapping("/ideas/{ideaId}/cancel")
    public ApiResponse<Void> cancelMilestone(@PathVariable Long ideaId) {
        adminMilestoneService.cancelMilestone(ideaId);
        return ApiResponse.ofSuccessWithoutBody();
    }
}
