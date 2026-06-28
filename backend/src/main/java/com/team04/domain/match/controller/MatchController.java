package com.team04.domain.match.controller;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.dto.request.ExpertMatchRespondRequest;
import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.match.dto.request.MatchRequest;
import com.team04.domain.match.dto.response.ExpertMatchResponse;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.match.service.ExpertMatchService;
import com.team04.domain.match.service.ExpertReviewService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

    private final ExpertMatchService expertMatchService;
    private final ExpertReviewService expertReviewService;
    private final IdeaRepository ideaRepository;

    /* 제안자 -> 전문가 매칭 요청 목록 조회 API */
    @GetMapping
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<List<ExpertMatchResponse>>> getMatches(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ExpertMatchResponse> response = expertMatchService.getMatches(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /* 전문가의 매칭 수락/거절 API */
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
    @GetMapping("/ideas/{ideaId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ExpertReviewResponse>>> getReviewsByIdeaId(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails.getRole() != Role.ADMIN) {
            Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                    .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
            if (!idea.getUserId().equals(userDetails.getUserId())) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }
        return ResponseEntity.ok(ApiResponse.ofSuccess(
                expertReviewService.getReviewsByIdeaId(ideaId)
        ));
    }
}
