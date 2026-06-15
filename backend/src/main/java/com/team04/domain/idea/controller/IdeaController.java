package com.team04.domain.idea.controller;

import com.team04.global.response.ApiResponse;
import com.team04.domain.idea.dto.request.CreateIdeaRequest;
import com.team04.domain.idea.dto.request.ReportIdeaRequest;
import com.team04.domain.idea.dto.request.UpdateIdeaRequest;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.dto.response.ReportIdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 아이디어 CRUD와 도용 신고 API를 제공하는 컨트롤러입니다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ideas")
public class IdeaController {

    private static final String USER_ID_HEADER = "X-USER-ID";

    private final IdeaService ideaService;

    // 인증 필요
    /** 로그인 사용자의 아이디어와 3단계 마일스톤을 등록합니다. */
    @PostMapping
    public ApiResponse<IdeaResponse> createIdea(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CreateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.createIdea(userId, request));
    }

    // 인증 필요 - Security 구현 후 로그인 여부 검증으로 대체
    /** 로그인 사용자만 접근 가능한 아이디어 상세 정보를 조회합니다. */
    @GetMapping("/{ideaId}")
    public ApiResponse<IdeaResponse> getIdea(
            @PathVariable Long ideaId,
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        return ApiResponse.ofSuccess(ideaService.getIdea(ideaId));
    }

    // 인증 필요
    /** 로그인 사용자가 본인의 심사 대기 아이디어 정보를 수정합니다. */
    @PutMapping("/{ideaId}")
    public ApiResponse<IdeaResponse> updateIdea(
            @PathVariable Long ideaId,
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody UpdateIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.updateIdea(ideaId, userId, request));
    }

    // 인증 필요
    /** 로그인 사용자가 본인의 심사 대기 아이디어를 소프트 삭제합니다. */
    @DeleteMapping("/{ideaId}")
    public ApiResponse<Void> deleteIdea(
            @PathVariable Long ideaId,
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        ideaService.deleteIdea(ideaId, userId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    // 인증 필요
    /** 로그인 사용자가 아이디어 도용 의심 신고를 접수합니다. */
    @PostMapping("/{ideaId}/reports")
    public ApiResponse<ReportIdeaResponse> reportIdea(
            @PathVariable Long ideaId,
            @RequestHeader(USER_ID_HEADER) Long reporterUserId,
            @Valid @RequestBody ReportIdeaRequest request
    ) {
        return ApiResponse.ofSuccess(ideaService.reportIdea(ideaId, reporterUserId, request));
    }
}