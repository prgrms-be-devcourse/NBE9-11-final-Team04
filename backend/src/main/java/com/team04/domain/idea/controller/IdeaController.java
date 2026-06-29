package com.team04.domain.idea.controller;

import com.team04.domain.idea.dto.request.*;
import com.team04.domain.idea.dto.response.*;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.user.entity.Role;
import com.team04.global.response.ApiResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 아이디어 CRUD, 임시저장, 프로젝트 목록 검색, 도용 신고 API를 제공하는 컨트롤러입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ideas")
@Tag(name = "아이디어 사용자 API", description = "아이디어 등록, 조회, 임시저장, 이미지, 계좌, 관심 프로젝트, 신고 API")
public class IdeaController {

    private final IdeaService ideaService;

    /** 로그인 사용자의 아이디어와 3단계 마일스톤을 등록합니다. */
    @Operation(
            summary = "아이디어 등록",
            description = "로그인 사용자의 아이디어와 3단계 마일스톤을 등록합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ApiResponse<IdeaResponse> createIdea(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.createIdea(userDetails.getUserId(), request));
    }

    /** 카테고리와 마감임박 필터, 정렬 조건으로 프로젝트 목록을 제공합니다. */
    @Operation(
            summary = "프로젝트 목록 조회",
            description = "카테고리, 마감임박 여부, 키워드, 정렬 조건으로 프로젝트 목록을 조회합니다."
    )
    @GetMapping
    public ApiResponse<Page<IdeaSummaryResponse>> getProjects(
            @RequestParam(required = false) IdeaCategory category,
            @RequestParam(required = false, defaultValue = "false") Boolean closingSoon,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(ideaService.getProjects(category, closingSoon, keyword, sort, pageable));
    }

    /** 신뢰도와 펀딩 지표 기반 인기 프로젝트 Top5를 제공합니다. */
    @Operation(
            summary = "인기 프로젝트 Top5 조회",
            description = "신뢰도와 펀딩 지표를 기반으로 인기 프로젝트 Top5를 조회합니다."
    )
    @GetMapping("/top5")
    public ApiResponse<List<IdeaResponse>> getTop5Ideas() {
        return ApiResponse.ofSuccess(ideaService.getTop5Ideas());
    }

    /** 로그인 사용자의 관심 프로젝트 목록을 Page 페이지네이션으로 제공합니다. */
    @Operation(
            summary = "관심 프로젝트 목록 조회",
            description = "로그인 사용자의 관심 프로젝트 목록을 페이지네이션으로 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/bookmarks")
    public ApiResponse<Page<IdeaResponse>> getBookmarks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(ideaService.getBookmarks(userDetails.getUserId(), pageable));
    }

    /** 보관 기간 내 로그인 사용자 본인의 임시저장 목록을 조회합니다. */
    @Operation(
            summary = "임시저장 목록 조회",
            description = "보관 기간 내 로그인 사용자 본인의 임시저장 목록을 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/drafts")
    public ApiResponse<List<IdeaDraftResponse>> getDrafts(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(ideaService.getDrafts(userDetails.getUserId()));
    }

    /** 로그인 사용자의 아이디어 임시저장을 생성합니다. */
    @Operation(
            summary = "아이디어 임시저장 생성",
            description = "로그인 사용자의 아이디어 임시저장을 생성합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/drafts")
    public ApiResponse<IdeaDraftResponse> createDraft(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody IdeaDraftRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.createDraft(userDetails.getUserId(), request));
    }

    /** 로그인 사용자가 본인의 임시저장 내용을 수정합니다. */
    @Operation(
            summary = "아이디어 임시저장 수정",
            description = "로그인 사용자가 본인의 임시저장 내용을 수정합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/drafts/{draftId}")
    public ApiResponse<IdeaDraftResponse> updateDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody IdeaDraftRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.updateDraft(draftId, userDetails.getUserId(), request));
    }

    /** 로그인 사용자가 본인의 임시저장을 삭제합니다. */
    @Operation(
            summary = "아이디어 임시저장 삭제",
            description = "로그인 사용자가 본인의 임시저장을 삭제합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/drafts/{draftId}")
    public ApiResponse<Void> deleteDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.deleteDraft(draftId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 본인의 임시저장에서 이어서 아이디어를 정식 등록합니다. */
    @Operation(
            summary = "임시저장 아이디어 등록",
            description = "로그인 사용자가 본인의 임시저장에서 이어서 아이디어를 정식 등록합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/drafts/{draftId}/publish")
    public ApiResponse<IdeaResponse> publishDraft(
            @PathVariable Long draftId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.publishDraft(draftId, userDetails.getUserId(), request));
    }

    /** 로그인 제안자가 아이디어 본문 이미지를 사전 업로드합니다. */
    @Operation(
            summary = "아이디어 본문 이미지 업로드",
            description = "로그인 제안자가 아이디어 본문 이미지를 사전 업로드합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/images")
    public ApiResponse<List<String>> uploadContentImages(
            @RequestPart("images") List<MultipartFile> images
    ) {
        return ApiResponse.ofSuccess(ideaService.uploadContentImages(images));
    }

    /** 아이디어 상태와 요청자 권한에 따라 상세 정보를 조회합니다.
     * OPEN 이전 아이디어는 작성자, 관리자, 매칭된 전문가만 조회 가능합니다. */
    @Operation(
            summary = "아이디어 상세 조회",
            description = "아이디어 상태와 요청자 권한에 따라 상세 정보를 조회합니다. OPEN 이전 아이디어는 작성자, 관리자, 매칭된 전문가만 조회할 수 있습니다."
    )
    @GetMapping("/{ideaId}")
    public ApiResponse<IdeaResponse> getIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;
        Role role = userDetails != null ? userDetails.getRole() : null;
        return ApiResponse.ofSuccess(ideaService.getIdea(ideaId, userId, role));
    }

    /** 로그인 사용자가 본인의 심사 대기 아이디어 정보를 수정합니다. */
    @Operation(
            summary = "아이디어 수정",
            description = "로그인 사용자가 본인의 심사 대기 아이디어 정보를 수정합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{ideaId}")
    public ApiResponse<IdeaResponse> updateIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.updateIdea(ideaId, userDetails.getUserId(), request));
    }

    /** 로그인 제안자가 본인의 심사 대기 아이디어 대표 이미지를 업로드합니다. */
    @Operation(
            summary = "아이디어 대표 이미지 업로드",
            description = "로그인 제안자가 본인의 심사 대기 아이디어 대표 이미지를 업로드합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{ideaId}/image")
    public ApiResponse<IdeaResponse> uploadIdeaImage(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.ofSuccess(ideaService.uploadIdeaImage(ideaId, userDetails.getUserId(), image));
    }

    /** 로그인 제안자가 관리자 최종 승인 전 정산 및 환불 계좌를 등록하거나 수정합니다. */
    @Operation(
            summary = "정산 및 환불 계좌 등록/수정",
            description = "로그인 제안자가 관리자 최종 승인 전 정산 및 환불 계좌를 등록하거나 수정합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{ideaId}/settlement-account")
    public ApiResponse<IdeaSettlementAccountResponse> upsertSettlementAccount(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody IdeaSettlementAccountRequest request
    ) {
        return ApiResponse.ofSuccess(
                ideaService.upsertSettlementAccount(ideaId, userDetails.getUserId(), request)
        );
    }

    /** 로그인 제안자가 본인 아이디어의 정산 및 환불 계좌 정보를 조회합니다. */
    @Operation(
            summary = "정산 및 환불 계좌 조회",
            description = "로그인 제안자가 본인 아이디어의 정산 및 환불 계좌 정보를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{ideaId}/settlement-account")
    public ApiResponse<IdeaSettlementAccountResponse> getSettlementAccount(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(ideaService.getSettlementAccount(ideaId, userDetails.getUserId()));
    }

    /** 로그인 사용자가 본인의 심사 대기 아이디어를 소프트 삭제합니다. */
    @Operation(
            summary = "아이디어 삭제",
            description = "로그인 사용자가 본인의 심사 대기 아이디어를 소프트 삭제합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{ideaId}")
    public ApiResponse<Void> deleteIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.deleteIdea(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 본인의 진행 중인 아이디어에 대해 취소를 신청합니다. */
    @Operation(
            summary = "아이디어 취소 신청",
            description = "로그인 사용자가 본인의 진행 중인 아이디어에 대해 취소를 신청합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{ideaId}/cancel")
    public ApiResponse<Void> requestCancellation(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.requestCancellation(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 아이디어를 관심 프로젝트로 저장합니다. */
    @Operation(
            summary = "관심 프로젝트 저장",
            description = "로그인 사용자가 아이디어를 관심 프로젝트로 저장합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{ideaId}/bookmark")
    public ApiResponse<Void> addBookmark(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.addBookmark(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 저장한 관심 프로젝트를 삭제합니다. */
    @Operation(
            summary = "관심 프로젝트 삭제",
            description = "로그인 사용자가 저장한 관심 프로젝트를 삭제합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{ideaId}/bookmark")
    public ApiResponse<Void> deleteBookmark(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ideaService.deleteBookmark(ideaId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    /** 로그인 사용자가 아이디어 도용 의심 신고를 접수합니다. */
    @Operation(
            summary = "아이디어 도용 의심 신고",
            description = "로그인 사용자가 아이디어 도용 의심 신고를 접수합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{ideaId}/reports")
    public ApiResponse<ReportIdeaResponse> reportIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReportIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.reportIdea(ideaId, userDetails.getUserId(), request));
    }

    /** 로그인 사용자가 등록한 본인 아이디어 목록을 조회합니다. */
    @Operation(
            summary = "내 아이디어 목록 조회",
            description = "로그인 사용자가 등록한 본인 아이디어 목록을 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<List<IdeaSummaryResponse>> getMyIdeas(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(ideaService.getMyIdeas(userDetails.getUserId()));
    }
}