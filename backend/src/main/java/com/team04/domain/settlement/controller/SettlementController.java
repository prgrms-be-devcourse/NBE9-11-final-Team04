package com.team04.domain.settlement.controller;

import com.team04.domain.settlement.dto.response.SettlementResponse;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
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

    // 프로젝트별 정산 이력 조회
    @GetMapping("/ideas/{ideaId}")
    public ApiResponse<List<SettlementResponse>> getSettlementsByIdea(@PathVariable Long ideaId) {
        return ApiResponse.ofSuccess(settlementService.getSettlementsByIdea(ideaId));
    }

    // 정산 단건 조회
    @GetMapping("/{settlementId}")
    public ApiResponse<SettlementResponse> getSettlement(@PathVariable Long settlementId) {
        return ApiResponse.ofSuccess(settlementService.getSettlement(settlementId));
    }
}