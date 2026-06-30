package com.team04.domain.settlement.service;

import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.settlement.dto.response.SettlementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final SettlementService settlementService;
    private final FundingService fundingService;

    /** 보증금 납부 확인을 기존 펀딩 서비스에 위임합니다. */
    @Transactional(readOnly = true)
    public DepositResponse getDeposit(Long ideaId) {
        return fundingService.getDepositAsAdmin(ideaId);
    }

    /** 보증금 환급 판정 흐름을 기존 정산 서비스에 위임합니다. */
    @Transactional
    public SettlementResponse releaseDeposit(Long ideaId) {
        return settlementService.createAdminDepositRefundSettlement(ideaId);
    }

    /** 보증금 몰수 판정 흐름을 기존 정산 서비스에 위임합니다. */
    @Transactional
    public SettlementResponse forfeitDeposit(Long ideaId) {
        return settlementService.forfeitDepositByAdmin(ideaId);
    }

    /** 에스크로 강제 환불 흐름을 기존 정산 서비스에 위임합니다. */
    @Transactional
    public void forceRefund(Long ideaId, String reason) {
        settlementService.forceRefund(ideaId, reason);
    }
}
