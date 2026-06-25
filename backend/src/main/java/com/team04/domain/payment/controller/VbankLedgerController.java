package com.team04.domain.payment.controller;

import com.team04.domain.payment.dto.response.VbankLedgerResponse;
import com.team04.domain.payment.service.VbankLedgerService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments/vbank-ledgers")
public class VbankLedgerController {

    private final VbankLedgerService vbankLedgerService;

    /**
     * 아이디어별 가상계좌 입출금 장부를 최신순으로 조회합니다.
     * 결제 도메인에서 관리하는 가상계좌 흐름이므로 /payments 하위 URL을 사용합니다.
     * 관리자, 제안자, 결제 성공 후원자만 접근 가능합니다.
     */
    @GetMapping("/ideas/{ideaId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<VbankLedgerResponse>> getLedgers(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(vbankLedgerService.getLedgers(
                ideaId,
                userDetails.getUserId(),
                userDetails.getRole()
        ));
    }
}
