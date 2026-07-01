package com.team04.domain.settlement.controller;

import com.team04.domain.payment.service.PaymentService;
import com.team04.domain.settlement.dto.request.PreSettlementRequest;
import com.team04.domain.settlement.dto.response.PreSettlementResponse;
import com.team04.domain.settlement.service.PreSettlementService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "선정산", description = "선정산 신청, 조회, 지급 콜백 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/pre-settlements")
public class PreSettlementController {

    private static final String WEBHOOK_SECRET_HEADER = "X-Webhook-Secret";

    private final PreSettlementService preSettlementService;
    private final PaymentService paymentService;

    /** 선정산을 신청합니다. 제안자만 가능하며 본인 프로젝트만 신청 가능합니다. */
    @Operation(
            summary = "선정산 신청",
            description = "제안자가 아이디어에 대해 선정산을 신청합니다. 선정산 한도는 아이디어 기준으로 검증됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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
     * 결제팀이 실제 지급 완료 후 콜백으로 호출합니다.
     */
    @Operation(
            summary = "선정산 완료 처리",
            description = "결제팀 지급 완료 콜백입니다. X-Webhook-Secret 검증 후 선정산 상태를 완료로 전환합니다."
    )
    @PatchMapping("/{preSettlementId}/complete")
    public ApiResponse<PreSettlementResponse> completePreSettlement(
            @PathVariable Long preSettlementId,
            @RequestHeader(value = WEBHOOK_SECRET_HEADER, required = false) String webhookSecret) {
        paymentService.verifyWebhookSecretOnly(webhookSecret);
        return ApiResponse.ofSuccess(preSettlementService.completePreSettlement(preSettlementId));
    }

    /**
     * 선정산 지급 실패 처리입니다.
     * 결제팀이 지급 실패 시 콜백으로 호출합니다.
     */
    @Operation(
            summary = "선정산 실패 처리",
            description = "결제팀 지급 실패 콜백입니다. X-Webhook-Secret 검증 후 선정산 상태를 실패로 전환합니다."
    )
    @PatchMapping("/{preSettlementId}/fail")
    public ApiResponse<PreSettlementResponse> failPreSettlement(
            @PathVariable Long preSettlementId,
            @RequestHeader(value = WEBHOOK_SECRET_HEADER, required = false) String webhookSecret) {
        paymentService.verifyWebhookSecretOnly(webhookSecret);
        return ApiResponse.ofSuccess(preSettlementService.failPreSettlement(preSettlementId));
    }

    /** 아이디어별 선정산 내역을 조회합니다. 관리자/제안자만 가능하며 제안자는 본인 프로젝트만 조회 가능합니다. */
    @Operation(
            summary = "선정산 목록 조회",
            description = "아이디어 ID 기준으로 선정산 신청/처리 내역을 조회합니다. 관리자와 제안자만 접근할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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
