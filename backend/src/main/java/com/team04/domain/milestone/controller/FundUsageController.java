package com.team04.domain.milestone.controller;

import com.team04.domain.milestone.dto.request.FundUsageRequest;
import com.team04.domain.milestone.dto.response.FundUsageResponse;
import com.team04.domain.milestone.service.FundUsageService;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.response.ApiResponse;
import com.team04.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fund-usages")
public class FundUsageController {

    private final FundUsageService fundUsageService;

    /** 자금 사용 내역을 입력합니다. 제안자만 가능하며 본인 프로젝트만 입력 가능합니다. */
    @Operation(summary = "자금 사용 내역 등록", description = "제안자가 선정산으로 실제 지급받은 금액 범위 안에서 자금 사용 내역을 등록합니다.")
    @PostMapping("/{ideaId}")
    public ApiResponse<FundUsageResponse> addFundUsage(
            @PathVariable Long ideaId,
            @Valid @RequestBody FundUsageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails.getRole() != Role.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(fundUsageService.addFundUsage(ideaId, request, userDetails.getUserId()));
    }

    /**
     * 자금 사용 내역을 조회합니다.
     * 관리자, 제안자(본인 프로젝트만), 결제 성공 후원자 조회 가능합니다.
     */
    @Operation(summary = "자금 사용 내역 조회", description = "아이디어별 자금 사용 내역을 조회합니다. 관리자, 제안자, 결제 성공 후원자만 접근할 수 있습니다.")
    @GetMapping("/{ideaId}")
    public ApiResponse<List<FundUsageResponse>> getFundUsages(
            @PathVariable Long ideaId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Role role = userDetails.getRole();
        if (role != Role.ADMIN && role != Role.USER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        return ApiResponse.ofSuccess(fundUsageService.getFundUsages(ideaId, userDetails.getUserId(), role));
    }
}
