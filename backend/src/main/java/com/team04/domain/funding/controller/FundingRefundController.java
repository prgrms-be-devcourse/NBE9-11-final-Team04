package com.team04.domain.funding.controller;

import com.team04.domain.funding.service.FundingRefundService;
import com.team04.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fundings")
public class FundingRefundController {

    private final FundingRefundService fundingRefundService;

    @PostMapping("/{fundingId}/refund")
    public ApiResponse<Void> refundFunding(@PathVariable Long fundingId) {
        fundingRefundService.refundFunding(fundingId);
        return ApiResponse.ofSuccessWithoutBody();
    }
}
