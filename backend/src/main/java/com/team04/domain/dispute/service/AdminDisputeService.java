package com.team04.domain.dispute.service;

import com.team04.domain.dispute.dto.request.AdminDisputeStatusRequest;
import com.team04.domain.dispute.dto.response.AdminDisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeResponse;
import com.team04.domain.dispute.dto.response.DisputeStatsResponse;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.entity.TargetType;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDisputeService {

    private final DisputeService disputeService;
    private final RefundService refundService;

    // 분쟁 현황 집계
    @Transactional(readOnly = true)
    public DisputeStatsResponse getDisputeStats() {
        return disputeService.getDisputeStats();
    }

    // 분쟁 목록 조회
    @Transactional(readOnly = true)
    public Page<AdminDisputeResponse> getDisputeList(
            DisputeStatus status, DisputeCategory category, TargetType targetType, Pageable pageable
    ) {
        return disputeService.getDisputeList(status, category, targetType, pageable);
    }

    // 분쟁 상태 변경
    @Transactional
    public DisputeResponse updateDisputeStatus(Long disputeId, AdminDisputeStatusRequest request) {
        return disputeService.updateDisputeStatus(disputeId, request);
    }

    // 강제 환불
    @Transactional
    public RefundResponse forceRefund(Long disputeId, Long paymentId) {
        return refundService.forceDisputeRefund(disputeId, paymentId);
    }
}