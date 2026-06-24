package com.team04.domain.settlement.controller;

import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 정산/보증금 관련 어드민 전용 API
 * 보증금 납부 확인, 환급/몰수 판정, 에스크로 강제 환불을 처리합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/settlements")
public class AdminSettlementController {

    private final SettlementService settlementService;
    private final FundingService fundingService;

    /** 보증금 납부 확인. 관리자만 가능합니다. */
    @GetMapping("/ideas/{ideaId}/deposit")
    public ApiResponse<DepositResponse> getDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAdmin(userDetails);
        return ApiResponse.ofSuccess(fundingService.getDeposit(ideaId));
    }

    /** 보증금 환급 판정. 관리자만 가능합니다. 정당한 사유 중단 또는 목표 미달성 시 제안자에게 환급합니다. */
    @PostMapping("/ideas/{ideaId}/deposit/release")
    public ApiResponse<DepositResponse> releaseDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAdmin(userDetails);
        return ApiResponse.ofSuccess(fundingService.releaseDeposit(ideaId));
    }

    /** 보증금 몰수 판정. 관리자만 가능합니다. 단순 포기/먹튀 시 보증금을 몰수합니다. */
    @PostMapping("/ideas/{ideaId}/deposit/forfeit")
    public ApiResponse<DepositResponse> forfeitDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAdmin(userDetails);
        return ApiResponse.ofSuccess(fundingService.forfeitDeposit(ideaId));
    }

    /**
     * 에스크로 강제 환불 처리. 관리자만 가능합니다.
     * 단순 포기/먹튀 케이스에서 관리자가 판정 시 호출합니다.
     * 후원금 잔액 환불 장부 + 보증금 몰수 장부 + 후원자 환불 레코드를 한 트랜잭션으로 생성합니다.
     */
    @PatchMapping("/ideas/{ideaId}/force-refund")
    public ApiResponse<Void> forceRefund(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        requireAdmin(userDetails);
        settlementService.forceRefund(ideaId);
        return ApiResponse.ofSuccessWithoutBody();
    }

    private void requireAdmin(CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}