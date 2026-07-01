package com.team04.domain.match.controller;

import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.match.dto.request.ExpertMatchRespondRequest;
import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.match.dto.request.MatchRequest;
import com.team04.domain.match.dto.response.ExpertMatchResponse;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.match.service.ExpertMatchService;
import com.team04.domain.match.service.ExpertReviewService;
import com.team04.domain.user.entity.Role;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "매칭", description = "전문가 매칭 요청, 수락/거절, 검토서 작성 API")
@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final ExpertMatchService expertMatchService;
    private final ExpertReviewService expertReviewService;
    private final IdeaService ideaService;

    /* 제안자 -> 전문가 매칭 요청 목록 조회 API */
    @Operation(
            summary = "매칭 요청 목록 조회",
            description = "전문가 본인에게 들어온 매칭 요청 목록을 조회합니다. EXPERT 권한 필요."
    )
    @GetMapping
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<List<ExpertMatchResponse>>> getMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ExpertMatchResponse> response = expertMatchService.getMatches(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /* 전문가의 매칭 수락/거절 API */
    @Operation(
            summary = "매칭 수락/거절",
            description = "전문가가 매칭 요청을 수락하거나 거절합니다. 거절 시 거절 사유는 필수입니다. EXPERT 권한 필요."
    )
    @PatchMapping("/{matchId}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertMatchResponse>> respond(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long matchId,
            @Valid @RequestBody ExpertMatchRespondRequest request
    ) {
        ExpertMatchResponse response = expertMatchService.respond(userDetails.getUserId(), matchId, request);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /* 전문가의 검토서 작성 API */
    @Operation(
            summary = "전문가 검토서 작성",
            description = "수락된 매칭에 대해 전문가가 구현 가능 여부, 예상 기간, 기술 스택, 리스크 요인, 검토 의견을 작성합니다. EXPERT 권한 필요."
    )
    @PostMapping("/{matchId}/review")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertReviewResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long matchId,
            @Valid @RequestBody ExpertReviewRequest request
    ) {
        ExpertReviewResponse response = expertReviewService.createReview(userDetails.getUserId(), matchId, request);
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }

    /* 제안자 -> 전문가 매칭 요청 API */
    @Operation(
            summary = "전문가 매칭 요청",
            description = "아이디어 제안자가 특정 전문가에게 매칭을 요청합니다. 거절 횟수 3회 초과 시 요청이 차단됩니다. USER 권한 필요."
    )
    @PostMapping("/experts/{expertProfileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ExpertMatchResponse>> requestMatch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long expertProfileId,
            @Valid @RequestBody MatchRequest request
    ) {
        ExpertMatchResponse response = expertMatchService.requestMatch(
                userDetails.getUserId(), expertProfileId, request
        );
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }

    /* 아이디어별 전문가 검토서 목록 조회 API */
    @Operation(
            summary = "아이디어별 전문가 검토서 목록 조회",
            description = "아이디어에 작성된 전문가 검토서 목록을 조회합니다. 관리자 또는 해당 아이디어 제안자만 접근 가능합니다."
    )
    @GetMapping("/ideas/{ideaId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ExpertReviewResponse>>> getReviewsByIdeaId(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // ADMIN이 아닌 경우 IdeaService 내부에서 소유자 검증 처리
        if (userDetails.getRole() != Role.ADMIN) {
            ideaService.getIdea(ideaId, userDetails.getUserId(), userDetails.getRole());
        }
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                expertReviewService.getReviewsByIdeaId(ideaId)
        ));
    }
}
