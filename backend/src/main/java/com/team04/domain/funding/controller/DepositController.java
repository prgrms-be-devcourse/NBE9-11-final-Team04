package com.team04.domain.funding.controller;

import com.team04.domain.funding.dto.request.PayDepositRequest;
import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.funding.service.DepositService;
import com.team04.domain.funding.service.FundingRefundService;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/deposits")
public class DepositController {

    private final DepositService depositService;

    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<DepositResponse> getDeposit(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(depositService.getDeposit(ideaId));
    }

    @PostMapping("/ideas/{ideaId}")
    public ApiResponse<DepositResponse> payDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PayDepositRequest request
    ) {
        return ApiResponse.ofSuccess(
                depositService.payDeposit(ideaId, userDetails.getUserId(), request)
        );
    }

    @PostMapping("/ideas/{ideaId}/release")
    public ApiResponse<DepositResponse> releaseDeposit(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ofSuccess(
                depositService.releaseDeposit(ideaId, userDetails.getUserId())
        );
    }
}
