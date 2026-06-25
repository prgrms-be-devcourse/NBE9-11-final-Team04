package com.team04.domain.settlement.service;

import com.team04.domain.dispute.repository.DisputeRepository;
import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundReason;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.RefundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Mock
    private DisputeRepository disputeRepository;

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

    @Test
    @DisplayName("이미 환불된 결제건은 제외하고 남은 후원자 기준으로 환불금을 재분배한다")
    void createCancelRefunds_excludesAlreadyRefundedPayments_andRedistributesToRemainingSponsors() {
        Payment payment1 = createPayment(1L, 101L, 30_000L);
        Payment payment2 = createPayment(2L, 102L, 30_000L);
        Payment payment3 = createPayment(3L, 103L, 40_000L);
        Refund alreadyRefunded = Refund.builder()
                .paymentId(1L)
                .sponsorId(1L)
                .amount(30_000L)
                .reason(RefundReason.CANCELLED)
                .build();

        given(paymentRepository.findPaymentsAndSponsorIdsToRefund(1L))
                .willReturn(List.of(
                        new Object[]{payment1, 1L},
                        new Object[]{payment2, 2L},
                        new Object[]{payment3, 3L}
                ));
        given(refundRepository.findByPaymentIdIn(Set.of(1L, 2L, 3L)))
                .willReturn(Set.of(alreadyRefunded));
        given(depositRepository.findByIdeaId(1L))
                .willReturn(java.util.Optional.of(Deposit.createHeld(1L, 10L, 30_000L)));
        given(preSettlementRepository.sumAmountByIdeaIdAndStatus(1L, PreSettlementStatus.COMPLETED))
                .willReturn(20_000L);

        refundService.createCancelRefunds(1L, false);

        ArgumentCaptor<List<Refund>> refundsCaptor = ArgumentCaptor.forClass(List.class);
        verify(refundRepository).saveAll(refundsCaptor.capture());
        List<Refund> refunds = refundsCaptor.getValue();

        assertThat(refunds)
                .extracting(Refund::getPaymentId)
                .containsExactly(2L, 3L);
        assertThat(refunds)
                .extracting(Refund::getSponsorId)
                .containsExactly(2L, 3L);
        assertThat(refunds)
                .extracting(Refund::getAmount)
                .containsExactly(36_429L, 43_571L);
        verify(fundingService).forfeitDeposit(1L);
        verify(fundingService, never()).releaseDeposit(1L);
    }

    private static Payment createPayment(Long id, Long fundingId, Long amount) {
        Payment payment = Payment.createPending(fundingId, "order-" + id, amount, PaymentTypes.PaymentMethod.CARD);
        setField(payment, "id", id);
        return payment;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
