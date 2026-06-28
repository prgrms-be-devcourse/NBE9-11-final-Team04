package com.team04.domain.admin.controller;

import com.team04.domain.dispute.dto.request.AdminDisputeStatusRequest;
import com.team04.domain.dispute.dto.request.ForceRefundRequest;
import com.team04.domain.dispute.dto.response.AdminDisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeStatsResponse;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.entity.TargetType;
import com.team04.domain.dispute.service.AdminDisputeService;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {

    private final AdminDisputeService adminDisputeService;

    /* 분쟁 현황 집계 API */
    @GetMapping("/stats")
    public ApiResponse<DisputeStatsResponse> getDisputeStats() {
        return ApiResponse.ofSuccess(adminDisputeService.getDisputeStats());
    }

    /* 분쟁 목록 조회 API */
    @GetMapping
    public ApiResponse<Page<AdminDisputeResponse>> getDisputeList(
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(required = false) DisputeCategory category,
            @RequestParam(required = false) TargetType targetType,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.ofSuccess(
                adminDisputeService.getDisputeList(status, category, targetType, pageable)
        );
    }

    /* 분쟁 상태 변경 API */
    @PatchMapping("/{disputeId}/status")
    public ApiResponse<DisputeResponse> updateDisputeStatus(
            @PathVariable Long disputeId,
            @RequestBody @Valid AdminDisputeStatusRequest request
    ) {
        return ApiResponse.ofSuccess(
                adminDisputeService.updateDisputeStatus(disputeId, request)
        );
    }

    /* 강제 환불 API */
    @PostMapping("/{disputeId}/force-refund")
    public ApiResponse<RefundResponse> forceRefund(
            @PathVariable Long disputeId,
            @RequestBody @Valid ForceRefundRequest request
    ) {
        return ApiResponse.ofSuccess(
                adminDisputeService.forceRefund(disputeId, request.paymentId())
        );
    }
}