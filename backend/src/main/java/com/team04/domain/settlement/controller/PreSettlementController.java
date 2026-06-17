package com.team04.domain.settlement.controller;

import com.team04.domain.settlement.dto.request.PreSettlementRequest;
import com.team04.domain.settlement.dto.response.PreSettlementResponse;
import com.team04.domain.settlement.service.PreSettlementService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pre-settlements")
public class PreSettlementController {

    private final PreSettlementService preSettlementService;

    /** 선정산을 신청합니다. 제안자만 가능합니다. */
    @PostMapping("/{milestoneId}")
    public ApiResponse<PreSettlementResponse> requestPreSettlement(
            @PathVariable Long milestoneId,
            @RequestBody PreSettlementRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.PROPOSER) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return ApiResponse.ofSuccess(preSettlementService.requestPreSettlement(milestoneId, request));
    }

    /**
     * 선정산 지급 완료 처리입니다.
     * TODO: 결제팀 Role 추가 후 권한 체크 필요 (현재 ADMIN으로 임시 처리)
     */
    @PatchMapping("/{preSettlementId}/complete")
    public ApiResponse<PreSettlementResponse> completePreSettlement(
            @PathVariable Long preSettlementId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // TODO: 결제팀 내부 서버 호출 방식 확정 후 인증 처리 필요 (현재 ADMIN으로 임시 처리)
        if (userDetails.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return ApiResponse.ofSuccess(preSettlementService.completePreSettlement(preSettlementId));
    }

    /** 마일스톤별 선정산 내역을 조회합니다. 관리자/제안자만 가능합니다. */
    @GetMapping("/{milestoneId}")
    public ApiResponse<List<PreSettlementResponse>> getPreSettlements(
            @PathVariable Long milestoneId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Role role = userDetails.getRole();
        if (role != Role.ADMIN && role != Role.PROPOSER) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }
        return ApiResponse.ofSuccess(preSettlementService.getPreSettlements(milestoneId));
    }
}