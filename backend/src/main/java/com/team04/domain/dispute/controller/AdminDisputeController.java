package com.team04.domain.dispute.controller;

import com.team04.domain.dispute.dto.request.AdminDisputeStatusRequest;
import com.team04.domain.dispute.dto.response.AdminDisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeStatsResponse;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.entity.TargetType;
import com.team04.domain.dispute.service.DisputeService;
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

    private final DisputeService disputeService;

    @GetMapping("/stats")
    public ApiResponse<DisputeStatsResponse> getDisputeStats() {
        return ApiResponse.ofSuccess(disputeService.getDisputeStats());
    }

    @GetMapping
    public ApiResponse<Page<AdminDisputeResponse>> getDisputeList(
            @RequestParam(required = false) DisputeStatus status,
            @RequestParam(required = false) DisputeCategory category,
            @RequestParam(required = false) TargetType targetType,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ofSuccess(disputeService.getDisputeList(status, category, targetType, pageable));
    }

    @PatchMapping("/{disputeId}/status")
    public ApiResponse<DisputeResponse> updateDisputeStatus(
            @PathVariable Long disputeId,
            @RequestBody @Valid AdminDisputeStatusRequest request) {
        return ApiResponse.ofSuccess(disputeService.updateDisputeStatus(disputeId, request));
    }
}
