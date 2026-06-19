package com.team04.domain.idea.controller;

import com.team04.domain.idea.dto.request.IdeaDraftRequest;
import com.team04.domain.idea.dto.response.IdeaDraftResponse;
import com.team04.domain.idea.dto.response.IdeaSummaryResponse;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.domain.idea.dto.request.CreateIdeaRequest;
import com.team04.domain.idea.dto.request.ReportIdeaRequest;
import com.team04.domain.idea.dto.request.UpdateIdeaRequest;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.dto.response.ReportIdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 아이디어 CRUD, 임시저장, 프로젝트 목록 검색, 도용 신고 API를 제공하는 컨트롤러입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ideas")
public class IdeaController {

    private final IdeaService ideaService;

    /** 로그인 사용자의 아이디어와 3단계 마일스톤을 등록합니다. */
    @PostMapping
    public ApiResponse<IdeaResponse> createIdea(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.createIdea(userDetails.getUserId(), request));
    }

    /** 카테고리와 마감임박 필터, 정렬 조건으로 프로젝트 목록을 제공합니다. */
    @GetMapping
    public ApiResponse<Slice<IdeaSummaryResponse>> getProjects(
            @RequestParam(required = false) IdeaCategory category,
            @RequestParam(required = false, defaultValue = "false") Boolean closingSoon,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(ideaService.getProjects(category, closingSoon, keyword, sort, pageable));
    }

    /** 신뢰도와 펀딩 지표 기반 인기 프로젝트 Top5를 제공합니다. */
    @GetMapping("/top5")
    public ApiResponse<List<IdeaResponse>> getTop5Ideas() {
        return ApiResponse.ofSuccess(ideaService.getTop5Ideas());
    }

    /** 로그인 사용자의 관심 프로젝트 목록을 Slice 페이지네이션으로 제공합니다. */
    @GetMapping("/bookmarks")
    public ApiResponse<Slice<IdeaResponse>> getBookmarks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(ideaService.getBookmarks(userDetails.getUserId(), pageable));
    }

    /** 보관 기간 내 로그인 사용자 본인의 임시저장 목록을 조회합니다. */
    @GetMapping("/drafts")
    public ApiResponse<List<IdeaDraftResponse>> getDrafts(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(ideaService.getDrafts(userDetails.getUserId()));
    }

    /** 로그인 사용자의 아이디어 임시저장을 생성합니다. */
    @PostMapping("/drafts")
    public ApiResponse<IdeaDraftResponse> createDraft(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody IdeaDraftRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.createDraft(userDetails.getUserId(), request));
    }

    /** 로그인 사용자가 본인의 임시저장 내용을 수정합니다. */
    @PutMapping("/drafts/{draftId}")
    public ApiResponse<IdeaDraftResponse> updateDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody IdeaDraftRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.updateDraft(draftId, userDetails.getUserId(), request));
    }

    /** 로그인 사용자가 본인의 임시저장을 삭제합니다. */
    @DeleteMapping("/drafts/{draftId}")
    public ApiResponse<Void> deleteDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.deleteDraft(draftId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 본인의 임시저장에서 이어서 아이디어를 정식 등록합니다. */
    @PostMapping("/drafts/{draftId}/publish")
    public ApiResponse<IdeaResponse> publishDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.publishDraft(draftId, userDetails.getUserId(), request));
    }

    /** 로그인 사용자만 접근 가능한 아이디어 상세 정보를 조회합니다. */
    @GetMapping("/{ideaId}")
    public ApiResponse<IdeaResponse> getIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(ideaService.getIdea(ideaId));
    }

    /** 로그인 사용자가 본인의 심사 대기 아이디어 정보를 수정합니다. */
    @PutMapping("/{ideaId}")
    public ApiResponse<IdeaResponse> updateIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.updateIdea(ideaId, userDetails.getUserId(), request));
    }

    /** 로그인 사용자가 본인의 심사 대기 아이디어를 소프트 삭제합니다. */
    @DeleteMapping("/{ideaId}")
    public ApiResponse<Void> deleteIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.deleteIdea(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 본인의 진행 중인 아이디어에 대해 취소를 신청합니다. */
    @PostMapping("/{ideaId}/cancel")
    public ApiResponse<Void> requestCancellation(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails.getRole() != Role.PROPOSER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        ideaService.requestCancellation(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 아이디어를 관심 프로젝트로 저장합니다. */
    @PostMapping("/{ideaId}/bookmark")
    public ApiResponse<Void> addBookmark(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.addBookmark(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 저장한 관심 프로젝트를 삭제합니다. */
    @DeleteMapping("/{ideaId}/bookmark")
    public ApiResponse<Void> deleteBookmark(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.deleteBookmark(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 아이디어 도용 의심 신고를 접수합니다. */
    @PostMapping("/{ideaId}/reports")
    public ApiResponse<ReportIdeaResponse> reportIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReportIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.reportIdea(ideaId, userDetails.getUserId(), request));
    }
}