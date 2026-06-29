package com.team04.domain.expert.controller;

import com.team04.domain.expert.dto.request.ExpertAppealRequest;
import com.team04.domain.expert.dto.request.ExpertProfileRequest;
import com.team04.domain.expert.dto.response.ExpertAppealResponse;
import com.team04.domain.expert.dto.response.ExpertProfileListResponse;
import com.team04.domain.expert.entity.TechStack;
import com.team04.domain.expert.service.ExpertAppealService;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.dto.response.ExpertProfileResponse;
import com.team04.domain.expert.dto.response.ExpertVerifyResponse;
import com.team04.domain.expert.service.ExpertProfileService;
import com.team04.domain.expert.service.ExpertVerifyService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "전문가 API", description = "전문가 자격 검증, 프로필 등록/조회, 소명 자료 제출 API")
@RestController
@RequestMapping("/experts")
@RequiredArgsConstructor
public class ExpertController {

    private final ExpertProfileService expertProfileService;
    private final ExpertVerifyService expertVerifyService;
    private final ExpertAppealService expertAppealService;


    /* 전문가 프로필 등록 API */
    @Operation(
            summary = "전문가 프로필 등록",
            description = "자격 검증이 완료된 전문가의 기술 스택, 포트폴리오, 경력을 등록합니다. 등록 완료 시 EXPERT 권한으로 변경됩니다."
    )
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
    @Operation(
            summary = "전문가 프로필 단건 조회",
            description = "전문가 프로필 ID로 전문가 상세 정보를 조회합니다. 로그인한 사용자만 접근 가능합니다."
    )
    @GetMapping("/{expertId}")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> getProfile(
            @PathVariable Long expertId
    ) {
        ExpertProfileResponse response = expertProfileService.getProfile(expertId);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /* 내 전문가 프로필 조회 API */
    @Operation(
            summary = "내 전문가 프로필 조회",
            description = "로그인한 전문가 본인의 프로필을 조회합니다. 로그인한 사용자만 접근 가능합니다."
    )
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ExpertProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ExpertProfileResponse response = expertProfileService.getMyProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }

    /* 전문가 프로필 수정 API */
    @Operation(
            summary = " 전문가 프로필 수정",
            description = "로그인한 전문가 본인의 기술 스택, 포트폴리오, 경력을 수정합니다. EXPERT 권한 필요."
    )
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
    @Operation(
            summary = "전문가 자격 검증",
            description = "사업자 등록번호(국세청 API) 또는 국가자격증(수동 검토)으로 전문가 자격을 검증합니다. 로그인한 사용자만 접근 가능합니다."
    )
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ExpertVerifyResponse>> verify(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExpertVerifyRequest request
    ) {
        ExpertVerifyResponse response = expertVerifyService.verify(userDetails.getUserId(), request);
        return ResponseEntity.status(201).body(ApiResponse.ofSuccess(response));
    }

    /* 격리 계정 소명 자료 제출 API */
    @Operation(
            summary = "격리 계정 소명 자료 제출",
            description = "격리된 전문가 계정이 소명 자료를 제출합니다. EXPERT 권한 필요. 파일 첨부는 선택 사항입니다."
    )
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
    @Operation(
            summary = "전문가 목록 조회",
            description = "기술 스택 필터와 페이징으로 ACTIVE 상태의 전문가 목록을 조회합니다. 로그인한 사용자만 접근 가능합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpertProfileListResponse>>> getProfiles(
            @RequestParam(required = false) TechStack techStack,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ExpertProfileListResponse> response = expertProfileService.getProfiles(techStack, pageable);
        return ResponseEntity.ok(ApiResponse.ofSuccess(response));
    }
}