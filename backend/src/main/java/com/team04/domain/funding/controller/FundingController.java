package com.team04.domain.funding.controller;

import com.team04.domain.funding.dto.request.OpenFundingRequest;
import com.team04.domain.funding.dto.request.PayDepositRequest;
import com.team04.domain.funding.dto.request.SponsorRequest;
import com.team04.domain.funding.dto.response.CreateFundingResponse;
import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.funding.dto.response.FundingDetailResponse;
import com.team04.domain.funding.dto.response.FundingSummaryResponse;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 펀딩 API 컨트롤러 — 보증금, 펀딩 오픈·조회, 후원 신청·취소, 달성률 SSE를 제공합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/fundings")
public class FundingController {

    private final FundingService fundingService;

    // 보증금 납부 (사업가 본인)
    @PostMapping("/{ideaId}/deposit")
    public ApiResponse<DepositResponse> payDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PayDepositRequest request
    ) {
        requireRole(userDetails, Role.USER);
        return ApiResponse.ofSuccess(fundingService.payDeposit(ideaId, userDetails.getUserId(), request));
    }

    // 펀딩 오픈 — 보증금 납부 확인 후 펀딩 시작 (사업가 본인)
    @PostMapping
    public ApiResponse<FundingDetailResponse> openFunding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OpenFundingRequest request
    ) {
        requireRole(userDetails, Role.USER);
        return ApiResponse.ofSuccess(fundingService.openFunding(request, userDetails.getUserId()));
    }

    // 펀딩 목록 조회 (OPEN/IN_PROGRESS, 페이징)
    @GetMapping
    public ApiResponse<Page<FundingSummaryResponse>> getFundings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(fundingService.getFundings(pageable));
    }

    // 펀딩 상세 조회 — 목표금액·달성률 포함 (fundingId = ideaId)
    @GetMapping("/{fundingId}")
    public ApiResponse<FundingDetailResponse> getFundingDetail(@PathVariable Long fundingId) {
        return ApiResponse.ofSuccess(fundingService.getFundingDetail(fundingId));
    }

    // 후원 신청 (결제) — Funding 생성 + 결제 세션 반환 (스폰서)
    @PostMapping("/{fundingId}/sponsors")
    public ApiResponse<CreateFundingResponse> applySponsorship(
            @PathVariable Long fundingId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SponsorRequest request
    ) {
        requireRole(userDetails, Role.USER);
        return ApiResponse.ofSuccess(
                fundingService.applySponsorship(fundingId, userDetails.getUserId(), request)
        );
    }

    // 후원 취소 — PAID 환불 또는 PENDING 결제 실패 처리 (스폰서)
    @DeleteMapping("/{fundingId}/sponsors/me")
    public ApiResponse<Void> cancelMySponsorship(
            @PathVariable Long fundingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        requireRole(userDetails, Role.USER);
        fundingService.cancelMySponsorship(fundingId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    // 달성률 실시간 스트리밍 (SSE)
    @GetMapping(value = "/{fundingId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeAchievement(@PathVariable Long fundingId) {
        return fundingService.subscribeAchievement(fundingId);
    }

    private void requireRole(CustomUserDetails userDetails, Role role) {
        if (userDetails.getRole() != role) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
