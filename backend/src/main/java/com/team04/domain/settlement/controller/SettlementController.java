package com.team04.domain.settlement.controller;

import com.team04.domain.settlement.dto.response.SettlementResponse;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Settlement", description = "최종 정산, 환불 정산, 보증금 처리 장부 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    /** 프로젝트 ID 기준으로 해당 프로젝트의 전체 정산 이력을 최신순으로 조회합니다. 관리자는 모든 프로젝트 조회 가능, 제안자는 본인 프로젝트만 조회 가능합니다. */
    @Operation(
            summary = "정산 목록 조회",
            description = "아이디어 ID 기준으로 최종 정산, 환불 정산, 보증금 처리 장부를 최신순으로 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<List<SettlementResponse>> getSettlementsByIdea(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(settlementService.getSettlementsByIdea(
                ideaId, userDetails.getUserId(), userDetails.getRole()));
    }

    /** 정산 ID로 단건 정산 정보를 조회합니다. 관리자는 모든 정산 조회 가능, 제안자는 본인 프로젝트 정산만 조회 가능합니다. */
    @Operation(
            summary = "정산 단건 조회",
            description = "정산 ID 기준으로 정산 유형, 총액, 플랫폼 수수료, 지급액, 상태를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{settlementId}")
    public ApiResponse<SettlementResponse> getSettlement(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ofSuccess(settlementService.getSettlement(
                settlementId, userDetails.getUserId(), userDetails.getRole()));
    }
}
