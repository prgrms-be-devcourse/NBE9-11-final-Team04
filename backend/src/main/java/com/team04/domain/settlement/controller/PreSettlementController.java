package com.team04.domain.settlement.controller;

import com.team04.domain.settlement.dto.request.PreSettlementRequest;
import com.team04.domain.settlement.dto.response.PreSettlementResponse;
import com.team04.domain.settlement.service.PreSettlementService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pre-settlements")
public class PreSettlementController {

    private final PreSettlementService preSettlementService;

    /** 선정산을 신청합니다. 제안자만 가능하며 본인 프로젝트만 신청 가능합니다. */
    @PostMapping("/ideas/{ideaId}")
    public ApiResponse<PreSettlementResponse> requestPreSettlement(
            @PathVariable Long ideaId,
            @Valid @RequestBody PreSettlementRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.USER) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return ApiResponse.ofSuccess(preSettlementService.requestPreSettlement(ideaId, request, userDetails.getUserId()));
    }

    /**
     * 선정산 지급 완료 처리입니다.
     * 결제팀이 실제 지급 완료 후 콜백으로 호출합니다. (ADMIN 인증)
     */
    @PatchMapping("/{preSettlementId}/complete")
    public ApiResponse<PreSettlementResponse> completePreSettlement(
            @PathVariable Long preSettlementId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return ApiResponse.ofSuccess(preSettlementService.completePreSettlement(preSettlementId));
    }

    /**
     * 선정산 지급 실패 처리입니다.
     * 결제팀이 지급 실패 시 콜백으로 호출합니다. (ADMIN 인증)
     */
    @PatchMapping("/{preSettlementId}/fail")
    public ApiResponse<PreSettlementResponse> failPreSettlement(
            @PathVariable Long preSettlementId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return ApiResponse.ofSuccess(preSettlementService.failPreSettlement(preSettlementId));
    }

    /** 아이디어별 선정산 내역을 조회합니다. 관리자/제안자만 가능하며 제안자는 본인 프로젝트만 조회 가능합니다. */
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<List<PreSettlementResponse>> getPreSettlements(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Role role = userDetails.getRole();
        if (role == Role.EXPERT) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return ApiResponse.ofSuccess(preSettlementService.getPreSettlements(ideaId, userDetails.getUserId(), role));
    }
}