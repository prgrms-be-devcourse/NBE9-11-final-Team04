package com.team04.domain.settlement.service;

import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.RefundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PreSettlementRepository preSettlementRepository;

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private IdeaService ideaService;

    @Mock
    private FundingService fundingService;

    @InjectMocks
    private RefundService refundService;

    @Test
    @DisplayName("정당한 사유 중단 시 보증금 상태 변경은 지급 성공 콜백에 위임한다")
    void createCancelRefunds_noRefundTarget_deferDepositRelease() {
        given(paymentRepository.findPaymentsAndSponsorIdsToRefund(1L)).willReturn(List.of());

        refundService.createCancelRefunds(1L, true);

        verify(fundingService, never()).releaseDeposit(1L);
        verify(fundingService, never()).forfeitDeposit(1L);
    }

    @Test
    @DisplayName("단순 포기 중단 시 환불 대상이 없어도 보증금을 몰수한다")
    void createCancelRefunds_noRefundTarget_forfeitDeposit() {
        given(paymentRepository.findPaymentsAndSponsorIdsToRefund(1L)).willReturn(List.of());

        refundService.createCancelRefunds(1L, false);

        verify(fundingService).forfeitDeposit(1L);
        verify(fundingService, never()).releaseDeposit(1L);
    }
}
