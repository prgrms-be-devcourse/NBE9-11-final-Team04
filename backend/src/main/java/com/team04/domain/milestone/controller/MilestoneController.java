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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;

    /** 프로젝트의 마일스톤 목록을 단계 순으로 조회합니다. 로그인한 사용자만 접근 가능합니다. */
    @GetMapping("/ideas/{ideaId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MilestoneResponse>> getMilestones(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(milestoneService.getMilestones(ideaId));
    }

    /** 마일스톤 단건을 조회합니다. 로그인한 사용자만 접근 가능합니다. */
    @GetMapping("/{milestoneId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MilestoneResponse> getMilestone(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(milestoneService.getMilestone(milestoneId));
    }

    /** 마일스톤의 완료/소명 보고서 목록을 최신순으로 조회합니다. 로그인한 사용자만 접근 가능합니다. */
    @GetMapping("/{milestoneId}/reports")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CompletionReportResponse>> getReports(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(milestoneService.getReports(milestoneId));
    }

    /**
     * 마일스톤 완료 보고서를 제출합니다. 제안자만 가능합니다.
     * 파일 첨부는 선택 사항입니다.
     * multipart/form-data 형식으로 요청합니다.
     * - request: JSON (content 필드)
     * - file: 첨부 파일 (선택)
     */
    @PostMapping(value = "/{milestoneId}/completion-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CompletionReportResponse> submitCompletionReport(
            @PathVariable Long milestoneId,
            @RequestPart("request") CompletionReportRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.PROPOSER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.submitCompletionReport(milestoneId, request, file));
    }

    /**
     * 소명 보고서를 제출합니다. 제안자만 가능합니다.
     * 파일 첨부는 선택 사항입니다.
     * multipart/form-data 형식으로 요청합니다.
     * - request: JSON (content 필드)
     * - file: 첨부 파일 (선택)
     */
    @PostMapping(value = "/{milestoneId}/appeal-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CompletionReportResponse> submitAppealReport(
            @PathVariable Long milestoneId,
            @RequestPart("request") CompletionReportRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.PROPOSER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.submitAppealReport(milestoneId, request, file));
    }

    /** 완료/소명 보고서를 승인합니다 (정상 진행). 관리자만 가능합니다. */
    @PostMapping("/{milestoneId}/reports/approve")
    public ApiResponse<CompletionReportResponse> approveReport(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.approveReport(milestoneId));
    }

    /** 완료/소명 보고서를 반려합니다. 관리자만 가능합니다. */
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
     * 소명 중단 인정 + 환불 처리입니다. 관리자만 가능합니다.
     * "더 이상 진행 못하겠다"는 소명을 관리자가 인정할 때 호출합니다.
     */
    @PostMapping("/{milestoneId}/reports/refund")
    public ApiResponse<Void> refundMilestone(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        milestoneService.refundMilestone(milestoneId);
        return ApiResponse.ofSuccess(null);
    }

    /**
     * 마일스톤 이행 중단 처리입니다. 관리자만 가능합니다.
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