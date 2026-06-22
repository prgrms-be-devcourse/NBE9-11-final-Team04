package com.team04.domain.funding.controller;

import com.team04.domain.funding.dto.request.CreateFundingRequest;
import com.team04.domain.funding.dto.response.CreateFundingResponse;
import com.team04.domain.funding.dto.response.FundingResponse;
import com.team04.domain.funding.service.FundingService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 후원(펀딩) API — 프로젝트별 후원 생성·조회 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/fundings")
public class FundingController {

    private final FundingService fundingService;

    // 프로젝트(idea)에 후원 요청 생성 + 결제 세션 반환
    @PostMapping("/ideas/{ideaId}")
    public ApiResponse<CreateFundingResponse> createFunding(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateFundingRequest request
    ) {
        return ApiResponse.ofSuccess(fundingService.createFunding(ideaId, userDetails.getUserId(), request));
    }

    // 후원 단건 조회
    @GetMapping("/{fundingId}")
    public ApiResponse<FundingResponse> getFunding(@PathVariable Long fundingId) {
        return ApiResponse.ofSuccess(fundingService.getFunding(fundingId));
    }

    // 프로젝트(idea)별 후원 목록 조회 (페이징)
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<Page<FundingResponse>> getFundingsByIdea(
            @PathVariable Long ideaId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(fundingService.getFundingsByIdea(ideaId, pageable));
    }
}
