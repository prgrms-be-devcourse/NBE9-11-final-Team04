package com.team04.domain.payment.controller;

import com.team04.domain.payment.dto.response.VbankLedgerResponse;
import com.team04.domain.payment.service.VbankLedgerService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "결제", description = "결제 생성·승인·환불·조회 및 PG 웹훅 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments/vbank-ledgers")
public class VbankLedgerController {

    private final VbankLedgerService vbankLedgerService;

    @Operation(
            summary = "가상계좌 장부 조회",
            description = "아이디어별 가상계좌 입출금 장부를 최신순으로 조회합니다. 관리자, 제안자, 결제 성공 후원자만 접근할 수 있습니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/ideas/{ideaId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<VbankLedgerResponse>> getLedgers(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ofSuccess(vbankLedgerService.getLedgers(
                ideaId,
                userDetails.getUserId(),
                userDetails.getRole()
        ));
    }
}
