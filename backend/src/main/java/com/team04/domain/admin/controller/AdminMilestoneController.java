package com.team04.domain.admin.controller;

import com.team04.domain.milestone.dto.request.RejectReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.service.AdminMilestoneService;
import com.team04.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "승인 대기 마일스톤 목록 조회", description = "관리자가 검토해야 하는 제출 완료 상태의 보고서가 있는 마일스톤 목록을 조회합니다.")
    @GetMapping("/pending-reports")
    public ApiResponse<List<MilestoneResponse>> getPendingReportMilestones() {
        return ApiResponse.ofSuccess(adminMilestoneService.getPendingReportMilestones());
    }

    /** 완료 보고서 승인 API */
    @Operation(summary = "완료 보고서 승인", description = "관리자가 완료 보고서를 승인합니다. 3단계 승인 시 아이디어 완료 및 최종 정산 흐름이 진행됩니다.")
    @PostMapping("/{milestoneId}/reports/approve/completion")
    public ApiResponse<CompletionReportResponse> approveCompletionReport(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(adminMilestoneService.approveCompletionReport(milestoneId));
    }

    /** 소명 보고서 승인 API */
    @Operation(summary = "소명 보고서 승인", description = "관리자가 소명 보고서를 승인합니다. 승인 시 마일스톤 진행 상태를 정책에 따라 복구하거나 다음 단계로 전환합니다.")
    @PostMapping("/{milestoneId}/reports/approve/appeal")
    public ApiResponse<CompletionReportResponse> approveAppealReport(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(adminMilestoneService.approveAppealReport(milestoneId));
    }

    /** 완료/소명 보고서 반려 API */
    @Operation(summary = "보고서 반려", description = "관리자가 완료 보고서 또는 소명 보고서를 사유와 함께 반려합니다.")
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
    @Operation(summary = "소명 중단 인정 및 환불", description = "관리자가 제안자의 중단 소명을 인정하고 후원자 환불 및 보증금 환급 정산 흐름을 생성합니다.")
    @PostMapping("/{milestoneId}/reports/refund")
    public ApiResponse<Void> refundMilestone(@PathVariable Long milestoneId) {
        adminMilestoneService.refundMilestone(milestoneId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 마일스톤 이행 중단 처리 API */
    @Operation(summary = "이행 중단 처리", description = "관리자가 아이디어의 마일스톤 이행을 중단 처리합니다.")
    @PostMapping("/ideas/{ideaId}/cancel")
    public ApiResponse<Void> cancelMilestone(@PathVariable Long ideaId) {
        adminMilestoneService.cancelMilestone(ideaId);
        return ApiResponse.ofSuccessWithoutBody();
    }
}
