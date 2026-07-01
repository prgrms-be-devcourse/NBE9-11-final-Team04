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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "펀딩", description = "보증금 납부, 펀딩 오픈·조회, 후원 신청·취소, 달성률 SSE API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/fundings")
public class FundingController {

    private final FundingService fundingService;

    @Operation(
            summary = "보증금 조회",
            description = "아이디어의 보증금 납부 상태를 조회합니다. 보증금이 없으면 404를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{ideaId}/deposit")
    public ApiResponse<DepositResponse> getDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(fundingService.getDeposit(ideaId, userDetails.getUserId()));
    }

    @Operation(
            summary = "보증금 납부",
            description = "아이디어에 대한 보증금을 납부합니다. 사업가(USER) 본인만 호출할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{ideaId}/deposit")
    public ApiResponse<DepositResponse> payDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PayDepositRequest request
    ) {
        requireRole(userDetails, Role.USER);
        return ApiResponse.ofSuccess(fundingService.payDeposit(ideaId, userDetails.getUserId(), request));
    }

    @Operation(
            summary = "펀딩 오픈",
            description = "보증금 납부 확인 후 펀딩을 시작합니다. 사업가(USER) 본인만 호출할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ApiResponse<FundingDetailResponse> openFunding(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OpenFundingRequest request
    ) {
        requireRole(userDetails, Role.USER);
        return ApiResponse.ofSuccess(fundingService.openFunding(request, userDetails.getUserId()));
    }

    @Operation(
            summary = "펀딩 목록 조회",
            description = "OPEN·IN_PROGRESS 상태의 펀딩 목록을 생성일 기준 내림차순으로 페이지네이션 조회합니다. 인증이 필요하지 않습니다."
    )
    @GetMapping
    public ApiResponse<Page<FundingSummaryResponse>> getFundings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(fundingService.getFundings(pageable));
    }

    @Operation(
            summary = "펀딩 상세 조회",
            description = "펀딩 ID(ideaId)로 목표금액·달성률 등 상세 정보를 조회합니다. 인증이 필요하지 않습니다."
    )
    @GetMapping("/{fundingId}")
    public ApiResponse<FundingDetailResponse> getFundingDetail(@PathVariable Long fundingId) {
        return ApiResponse.ofSuccess(fundingService.getFundingDetail(fundingId));
    }

    @Operation(
            summary = "후원 신청",
            description = "펀딩에 후원을 신청하고 결제 세션을 생성합니다. 스폰서(USER)만 호출할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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

    @Operation(
            summary = "내 후원 취소",
            description = "본인의 후원을 취소합니다. PAID 상태는 환불, PENDING 상태는 결제 실패 처리됩니다. 스폰서(USER)만 호출할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{fundingId}/sponsors/me")
    public ApiResponse<Void> cancelMySponsorship(
            @PathVariable Long fundingId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        requireRole(userDetails, Role.USER);
        fundingService.cancelMySponsorship(fundingId, userDetails.getUserId());
        return ApiResponse.ofSuccessWithoutBody();
    }

    @Operation(
            summary = "달성률 실시간 구독",
            description = "펀딩 달성률 변화를 Server-Sent Events(SSE)로 실시간 수신합니다. 인증이 필요하지 않습니다."
    )
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
