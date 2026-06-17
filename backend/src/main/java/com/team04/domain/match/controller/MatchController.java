package com.team04.domain.match.controller;

import com.team04.domain.match.dto.request.ExpertMatchRespondRequest;
import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.match.dto.response.ExpertMatchResponse;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.match.service.ExpertMatchService;
import com.team04.domain.match.service.ExpertReviewService;
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
@RequestMapping("/experts/matches")
@RequiredArgsConstructor
public class MatchController {

    private final ExpertMatchService expertMatchService;
    private final ExpertReviewService expertReviewService;

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

}
