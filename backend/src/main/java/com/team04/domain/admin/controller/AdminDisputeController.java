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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 분쟁 API", description = "관리자 분쟁 목록 조회, 상태 변경, 강제 환불 API")
@RestController
@RequestMapping("/admin/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {

    private final AdminDisputeService adminDisputeService;

    /* 분쟁 현황 집계 API */
    @Operation(
            summary = "분쟁 현황 집계",
            description = "전체 분쟁 상태별 건수를 집계합니다."
    )
    @GetMapping("/stats")
    public ApiResponse<DisputeStatsResponse> getDisputeStats() {
        return ApiResponse.ofSuccess(adminDisputeService.getDisputeStats());
    }

    /* 분쟁 목록 조회 API */
    @Operation(
            summary = "분쟁 목록 조회",
            description = "상태, 카테고리, 대상 타입 필터로 분쟁 목록을 조회합니다."
    )
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
    @Operation(
            summary = "분쟁 상태 변경",
            description = "관리자가 분쟁 상태를 변경합니다. RESOLVED 시 신고 인정, REJECTED 시 신고 기각 처리됩니다."
    )
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
    @Operation(
            summary = "강제 환불",
            description = "RESOLVED 상태의 IDEA 분쟁에서 특정 후원자 결제를 관리자가 수동 환불합니다."
    )
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