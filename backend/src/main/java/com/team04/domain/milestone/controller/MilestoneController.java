package com.team04.domain.milestone.controller;

import com.team04.domain.milestone.dto.request.CompletionReportRequest;
import com.team04.domain.milestone.dto.response.CompletionReportResponse;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.service.MilestoneService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    /** 프로젝트의 마일스톤 목록을 단계 순으로 조회합니다. */
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<List<MilestoneResponse>> getMilestones(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(milestoneService.getMilestones(ideaId));
    }

    /** 마일스톤 단건을 조회합니다. */
    @GetMapping("/{milestoneId}")
    public ApiResponse<MilestoneResponse> getMilestone(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(milestoneService.getMilestone(milestoneId));
    }

    /** 마일스톤의 완료/소명 보고서 목록을 최신순으로 조회합니다. */
    @GetMapping("/{milestoneId}/reports")
    public ApiResponse<List<CompletionReportResponse>> getReports(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(milestoneService.getReports(milestoneId));
    }

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

    /** 완료 보고서를 승인합니다. 관리자만 가능합니다. 3단계 승인 시 최종 정산, 미만 시 다음 단계 자동 시작됩니다. */
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

    /**
     * 마일스톤 이행 중단 처리입니다. 관리자만 가능합니다.
     * 현재 진행 중인 마일스톤을 취소하고 환불 장부를 생성합니다.
     */
    @PostMapping("/ideas/{ideaId}/cancel")
    public ApiResponse<Void> cancelMilestone(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        milestoneService.cancelMilestone(ideaId);
        return ApiResponse.ofSuccess(null);
    }
}