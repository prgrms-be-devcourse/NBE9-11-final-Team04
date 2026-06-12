package com.team04.funding.controller;

import com.team04.funding.dto.request.CreateFundingRequest;
import com.team04.funding.dto.response.FundingResponse;
import com.team04.funding.service.FundingService;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 후원(펀딩) API — 프로젝트별 후원 생성·조회 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/fundings")
public class FundingController {

    private final FundingService fundingService;

    // 프로젝트(idea)에 후원 요청 생성
    @PostMapping("/ideas/{ideaId}")
    public ApiResponse<FundingResponse> createFunding(
            @PathVariable Long ideaId,
            @Valid @RequestBody CreateFundingRequest request
    ) {
        return ApiResponse.ofSuccess(fundingService.createFunding(ideaId, request));
    }

    // 후원 단건 조회
    @GetMapping("/{fundingId}")
    public ApiResponse<FundingResponse> getFunding(@PathVariable Long fundingId) {
        return ApiResponse.ofSuccess(fundingService.getFunding(fundingId));
    }

    // 프로젝트(idea)별 후원 목록 조회
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<List<FundingResponse>> getFundingsByIdea(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(fundingService.getFundingsByIdea(ideaId));
    }
}
