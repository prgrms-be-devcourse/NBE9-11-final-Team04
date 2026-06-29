package com.team04.domain.admin.controller;

import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.settlement.dto.request.ForceRefundRequest;
import com.team04.domain.settlement.dto.response.SettlementResponse;
import com.team04.domain.settlement.service.AdminSettlementService;
import com.team04.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 정산/보증금 관련 어드민 전용 API
 * 보증금 납부 확인, 환급/몰수 판정, 에스크로 강제 환불을 처리합니다.
 */
@Tag(name = "Admin Settlement", description = "관리자 정산, 보증금, 강제 환불 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/settlements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    /** 보증금 납부 확인. 관리자만 가능합니다. */
    @Operation(
            summary = "보증금 납부 확인",
            description = "아이디어 ID 기준으로 보증금 납부 상태와 금액을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/ideas/{ideaId}/deposit")
    public ApiResponse<DepositResponse> getDeposit(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(adminSettlementService.getDeposit(ideaId));
    }

    /**
     * 보증금 환급 판정. 관리자만 가능합니다.
     * 정산 장부와 지급 요청을 생성하고, 지급 성공 콜백 이후 Deposit 상태를 REFUNDED로 전환합니다.
     */
    @Operation(
            summary = "보증금 환급 판정",
            description = "관리자가 보증금 환급 정산 장부와 지급 요청을 생성합니다. 지급 성공 이후 보증금 상태가 환급 완료로 전환됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/ideas/{ideaId}/deposit/release")
    public ApiResponse<SettlementResponse> releaseDeposit(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(adminSettlementService.releaseDeposit(ideaId));
    }

    /**
     * 보증금 몰수 판정. 관리자만 가능합니다.
     * 정산 장부와 가상계좌 공개 장부를 남긴 뒤 Deposit 상태를 FORFEITED로 전환합니다.
     */
    @Operation(
            summary = "보증금 몰수 판정",
            description = "관리자가 보증금 몰수 정산 장부와 가상계좌 공개 장부를 생성하고 보증금 상태를 몰수로 전환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/ideas/{ideaId}/deposit/forfeit")
    public ApiResponse<SettlementResponse> forfeitDeposit(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(adminSettlementService.forfeitDeposit(ideaId));
    }

    /**
     * 에스크로 강제 환불 처리. 관리자만 가능합니다.
     * 단순 포기/먹튀 케이스에서 관리자가 판정 시 호출합니다.
     * 후원금 잔액 환불 장부 + 보증금 몰수 장부 + 후원자 환불 레코드를 한 트랜잭션으로 생성합니다.
     */
    @Operation(
            summary = "에스크로 강제 환불",
            description = "관리자가 강제 환불 사유와 함께 후원금 잔액 환불 장부, 보증금 몰수 장부, 후원자 환불 레코드를 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/ideas/{ideaId}/force-refund")
    public ApiResponse<Void> forceRefund(
            @PathVariable Long ideaId,
            @Valid @RequestBody ForceRefundRequest request
    ) {
        adminSettlementService.forceRefund(ideaId, request.reason());
        return ApiResponse.ofSuccessWithoutBody();
    }
}
