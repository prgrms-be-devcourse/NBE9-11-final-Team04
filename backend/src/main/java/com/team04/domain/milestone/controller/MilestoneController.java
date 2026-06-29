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
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "마일스톤 목록 조회", description = "아이디어 ID 기준으로 1~3단계 마일스톤 목록을 단계 순으로 조회합니다.")
    @GetMapping("/ideas/{ideaId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MilestoneResponse>> getMilestones(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(milestoneService.getMilestones(ideaId));
    }

    /** 마일스톤 단건을 조회합니다. 로그인한 사용자만 접근 가능합니다. */
    @Operation(summary = "마일스톤 단건 조회", description = "마일스톤 ID 기준으로 단일 마일스톤 정보를 조회합니다.")
    @GetMapping("/{milestoneId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MilestoneResponse> getMilestone(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(milestoneService.getMilestone(milestoneId));
    }

    /** 마일스톤의 완료/소명 보고서 목록을 최신순으로 조회합니다. 로그인한 사용자만 접근 가능합니다. */
    @Operation(summary = "보고서 목록 조회", description = "마일스톤에 제출된 완료 보고서와 소명 보고서 목록을 최신순으로 조회합니다.")
    @GetMapping("/{milestoneId}/reports")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CompletionReportResponse>> getReports(@PathVariable Long milestoneId) {
        return ApiResponse.ofSuccess(milestoneService.getReports(milestoneId));
    }

    /** 완료/소명 보고서 단건을 조회합니다. 로그인한 사용자만 접근 가능합니다. */
    @Operation(summary = "보고서 단건 조회", description = "마일스톤 ID와 보고서 ID 기준으로 완료/소명 보고서 단건을 조회합니다.")
    @GetMapping("/{milestoneId}/reports/{reportId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CompletionReportResponse> getReport(
            @PathVariable Long milestoneId,
            @PathVariable Long reportId) {
        return ApiResponse.ofSuccess(milestoneService.getReport(milestoneId, reportId));
    }

    /**
     * 마일스톤 완료 보고서를 제출합니다. 제안자만 가능합니다.
     * 파일 첨부는 선택 사항입니다.
     * multipart/form-data 형식으로 요청합니다.
     */
    @Operation(summary = "완료 보고서 제출", description = "제안자가 현재 진행 중인 마일스톤의 완료 보고서를 제출합니다. 파일 첨부는 선택 사항입니다.")
    @PostMapping(value = "/{milestoneId}/completion-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CompletionReportResponse> submitCompletionReport(
            @PathVariable Long milestoneId,
            @RequestPart("request") CompletionReportRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.submitCompletionReport(milestoneId, request, file));
    }

    /**
     * 소명 보고서를 제출합니다. 제안자만 가능합니다.
     * 파일 첨부는 선택 사항입니다.
     * multipart/form-data 형식으로 요청합니다.
     */
    @Operation(summary = "소명 보고서 제출", description = "완료 보고서 반려 이후 제안자가 소명 보고서를 제출합니다. 파일 첨부는 선택 사항입니다.")
    @PostMapping(value = "/{milestoneId}/appeal-reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CompletionReportResponse> submitAppealReport(
            @PathVariable Long milestoneId,
            @RequestPart("request") CompletionReportRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(milestoneService.submitAppealReport(milestoneId, request, file));
    }

}
