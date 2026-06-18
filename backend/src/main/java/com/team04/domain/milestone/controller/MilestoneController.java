package com.team04.domain.milestone.controller;

import com.team04.domain.milestone.dto.request.CompletionReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.service.MilestoneService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    /** 마일스톤 완료 보고서를 제출합니다. 제안자만 가능합니다. */
    @PostMapping("/{milestoneId}/completion-reports")
    public ApiResponse<CompletionReportResponse> submitCompletionReport(
            @PathVariable Long milestoneId,
            @RequestBody CompletionReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.PROPOSER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.submitCompletionReport(milestoneId, request));
    }

    /** 소명 보고서를 제출합니다. 제안자만 가능합니다. */
    @PostMapping("/{milestoneId}/appeal-reports")
    public ApiResponse<CompletionReportResponse> submitAppealReport(
            @PathVariable Long milestoneId,
            @RequestBody CompletionReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.PROPOSER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.submitAppealReport(milestoneId, request));
    }

    /** 완료 보고서를 승인합니다. 관리자만 가능합니다. 3단계 승인 시 최종 정산이 생성됩니다. */
    @PostMapping("/{milestoneId}/reports/approve")
    public ApiResponse<CompletionReportResponse> approveReport(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.approveReport(milestoneId));
    }

    /** 완료 보고서를 반려합니다. 관리자만 가능합니다. */
    @PostMapping("/{milestoneId}/reports/reject")
    public ApiResponse<CompletionReportResponse> rejectReport(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.rejectReport(milestoneId));
    }
}