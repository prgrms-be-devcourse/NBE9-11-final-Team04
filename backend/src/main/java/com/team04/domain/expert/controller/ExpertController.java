package com.team04.domain.expert.controller;

import com.team04.domain.expert.dto.request.ExpertAppealRequest;
import com.team04.domain.expert.dto.request.ExpertProfileRequest;
import com.team04.domain.expert.dto.response.ExpertAppealResponse;
import com.team04.domain.expert.dto.response.ExpertProfileListResponse;
import com.team04.domain.expert.entity.ExpertAppeal;
import com.team04.domain.expert.entity.TechStack;
import com.team04.domain.expert.service.ExpertAppealService;
import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.dto.response.ExpertProfileResponse;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.expert.dto.response.ExpertVerifyResponse;
import com.team04.domain.expert.service.ExpertProfileService;
import com.team04.domain.match.service.ExpertReviewService;
import com.team04.domain.expert.service.ExpertVerifyService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertProfileService expertProfileService;
    private final ExpertVerifyService expertVerifyService;
    private final ExpertAppealService expertAppealService;


    /* 전문가 프로필 등록 API */
    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> registerProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExpertProfileRequest request
    ) {
        ExpertProfileResponse response = expertProfileService.registerProfile(userDetails.getUserId(), request);
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }


    /* 전문가 프로필 조회 API */
    @GetMapping("/{expertId}")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> getProfile(
            @PathVariable Long expertId
    ) {
        ExpertProfileResponse response = expertProfileService.getProfile(expertId);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }


    /* 내 전문가 프로필 조회 API */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ExpertProfileResponse response = expertProfileService.getMyProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /* 내 전문가 프로필 수정 API */
    @PatchMapping("/profile")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExpertProfileRequest request
    ) {
        ExpertProfileResponse response = expertProfileService.updateProfile(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }


    /* 전문가 프로필 검증 API */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<ExpertVerifyResponse>> verify(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExpertVerifyRequest request
    ) {
        ExpertVerifyResponse response = expertVerifyService.verify(userDetails.getUserId(), request);
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }

    /* 격리 계정 소명 자료 제출 API */
    @PostMapping("/appeal")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<ExpertAppealResponse>> submitAppeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("data") @Valid ExpertAppealRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        ExpertAppealResponse response = expertAppealService.submitAppeal(
                userDetails.getUserId(), request, file
        );
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }

    /* 카테고리별 ACTIVE 상태의 전문가 목록 조회 API */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpertProfileListResponse>>> getProfiles(
            @RequestParam(required = false) TechStack techStack,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ExpertProfileListResponse> response = expertProfileService.getProfiles(techStack, pageable);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }
}