package com.team04.domain.admin.controller;

import com.team04.domain.milestone.dto.request.RejectReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.service.AdminMilestoneService;
import com.team04.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Milestone", description = "관리자 마일스톤 검토 및 중단 처리 API")
@RestController
@RequestMapping("/admin/milestones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMilestoneController {

    private final AdminMilestoneService adminMilestoneService;

    /** 관리자 검토 대기 마일스톤 목록 조회 API */
    @Operation(
            summary = "승인 대기 마일스톤 목록 조회",
            description = "관리자가 검토해야 하는 SUBMITTED 상태 보고서가 있는 마일스톤 목록을 오래된 제출순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/pending-reports")
    public ApiResponse<List<MilestoneResponse>> getPendingReportMilestones() {
        return ApiResponse.ofSuccess(adminMilestoneService.getPendingReportMilestones());
    }

    /** 완료 보고서 승인 API */
    @Operation(
            summary = "완료 보고서 승인",
            description = "관리자가 완료 보고서를 승인합니다. 3단계 승인 시 최종 정산과 보증금 환급 정산이 생성됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{milestoneId}/reports/approve/completion")
    public ApiResponse<CompletionReportResponse> approveCompletionReport(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(adminMilestoneService.approveCompletionReport(milestoneId));
    }

    /** 소명 보고서 승인 API */
    @Operation(
            summary = "소명 보고서 승인",
            description = "관리자가 소명 보고서를 승인합니다. 3단계 승인 시 프로젝트 완료 흐름으로 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{milestoneId}/reports/approve/appeal")
    public ApiResponse<CompletionReportResponse> approveAppealReport(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(adminMilestoneService.approveAppealReport(milestoneId));
    }

    /** 완료/소명 보고서 반려 API */
    @Operation(
            summary = "보고서 반려",
            description = "관리자가 최신 제출 보고서를 반려하고 반려 사유를 저장합니다. 소명 보고서가 3회 반려되면 중단 및 보증금 몰수 흐름으로 전환됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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
    @Operation(
            summary = "소명 중단 인정 및 환불",
            description = "관리자가 제안자의 중단 소명을 인정하고 정당한 사유 중단 환불 흐름을 실행합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{milestoneId}/reports/refund")
    public ApiResponse<Void> refundMilestone(@PathVariable Long milestoneId) {
        adminMilestoneService.refundMilestone(milestoneId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 마일스톤 이행 중단 처리 API */
    @Operation(
            summary = "마일스톤 이행 중단 처리",
            description = "관리자가 아이디어의 진행 중 마일스톤을 중단하고 부정/미소명 중단 환불 흐름을 실행합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/ideas/{ideaId}/cancel")
    public ApiResponse<Void> cancelMilestone(@PathVariable Long ideaId) {
        adminMilestoneService.cancelMilestone(ideaId);
        return ApiResponse.ofSuccessWithoutBody();
    }
}
