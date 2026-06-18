package com.team04.domain.settlement.controller;

import com.team04.domain.settlement.dto.response.SettlementResponse;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    /** 프로젝트 ID 기준으로 해당 프로젝트의 전체 정산 이력을 최신순으로 조회합니다. 관리자는 모든 프로젝트 조회 가능, 제안자는 본인 프로젝트만 조회 가능합니다. */
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<List<SettlementResponse>> getSettlementsByIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(settlementService.getSettlementsByIdea(
                ideaId, userDetails.getUserId(), userDetails.getRole()));
    }
    /** 정산 ID로 단건 정산 정보를 조회합니다. 관리자는 모든 정산 조회 가능, 제안자는 본인 프로젝트 정산만 조회 가능합니다. */
    @GetMapping("/{settlementId}")
    public ApiResponse<SettlementResponse> getSettlement(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(settlementService.getSettlement(
                settlementId, userDetails.getUserId(), userDetails.getRole()));
    }
}