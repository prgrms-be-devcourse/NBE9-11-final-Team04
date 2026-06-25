package com.team04.domain.idea.controller;

import com.team04.domain.idea.dto.request.AdminIdeaRejectRequest;
import com.team04.domain.idea.dto.response.AdminIdeaReviewResponse;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.service.IdeaAdminService;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 관리자 아이디어 심사 목록, 승인, 반려, 상태 통계 API를 제공하는 컨트롤러입니다. */
@RestController
@RequestMapping("/admin/ideas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminIdeaController {

    private final IdeaAdminService ideaAdminService;

    /** 요청한 상태의 관리자 아이디어 심사 목록을 페이지로 조회합니다. */
    @GetMapping
    public ApiResponse<Page<AdminIdeaReviewResponse>> getReviews(
            @RequestParam IdeaStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(ideaAdminService.getReviews(status, pageable));
    }

    /** 관리자가 아이디어를 승인 상태로 전이합니다. */
    @PatchMapping("/{ideaId}/approve")
    public ApiResponse<Void> approve(@PathVariable Long ideaId) {
        ideaAdminService.approve(ideaId);
        return ApiResponse.ofSuccessWithoutBody();
    }


    /** 관리자가 아이디어를 반려하고 반려 사유를 저장합니다. */
    @PatchMapping("/{ideaId}/reject")
    public ApiResponse<Void> reject(@PathVariable Long ideaId,
                                    @RequestBody @Valid AdminIdeaRejectRequest request) {
        ideaAdminService.reject(ideaId, request.reason());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 관리자가 OPEN 또는 IN_PROGRESS 아이디어를 일시 중단합니다. */
    @PatchMapping("/{ideaId}/suspend")
    public ApiResponse<Void> suspendIdea(@PathVariable Long ideaId) {
        ideaAdminService.suspendIdea(ideaId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 관리자가 일시 중단된 아이디어를 중단 전 상태로 복원합니다. */
    @PatchMapping("/{ideaId}/restore")
    public ApiResponse<Void> restoreIdea(@PathVariable Long ideaId) {
        ideaAdminService.restoreIdea(ideaId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 전체 아이디어 상태별 현황을 집계합니다. */
    @GetMapping("/stats")
    public ApiResponse<Map<IdeaStatus, Long>> getStatusStats() {
        return ApiResponse.ofSuccess(ideaAdminService.getStatusStats());
    }
}

