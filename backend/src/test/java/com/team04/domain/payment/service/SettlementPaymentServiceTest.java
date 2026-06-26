package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.dto.request.PayoutRequest;
import com.team04.domain.payment.dto.request.PayoutTargetType;
import com.team04.domain.payment.dto.response.PaymentRefundResult;
import com.team04.domain.payment.dto.response.PayoutResult;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.event.PreSettlementPayoutRequestedEvent;
import com.team04.domain.payment.event.SettlementPayoutRequestedEvent;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundReason;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import com.team04.domain.settlement.entity.SettlementType;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.RefundRepository;
import com.team04.domain.settlement.repository.SettlementRepository;
import com.team04.domain.settlement.service.PreSettlementService;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.config.payment.PaymentProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementPaymentServiceTest {

    @InjectMocks
    private SettlementPaymentService settlementPaymentService;

    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentPayoutService paymentPayoutService;
    @Mock
    private PaymentProperties paymentProperties;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private IdeaService ideaService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private PreSettlementRepository preSettlementRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private PreSettlementService preSettlementService;
    @Mock
    private RefundService refundService;
    @Mock
    private SettlementService settlementService;
    @Mock
    private VbankLedgerService vbankLedgerService;

    @Test
    @DisplayName("선정산 지급 성공 시 complete 콜백 호출")
    void processPreSettlementPayout_success() {
        PreSettlement preSettlement = PreSettlement.builder()
                .ideaId(10L)
                .amount(50_000L)
                .build();
        setField(preSettlement, "id", 1L);

        given(preSettlementRepository.findById(1L)).willReturn(Optional.of(preSettlement));
        given(ideaService.getIdea(10L)).willReturn(sampleIdea(10L, 100L));
        given(userRepository.findById(100L)).willReturn(Optional.of(sampleUser(100L)));
        given(paymentProperties.payout()).willReturn(new PaymentProperties.Payout(true, "088", "000", ""));
        given(paymentPayoutService.payout(any(PayoutRequest.class)))
                .willReturn(PayoutResult.success("mock-payout-1"));

        settlementPaymentService.processPreSettlementPayout(1L);

        verify(preSettlementService).completePreSettlement(1L);
        verify(preSettlementService, never()).failPreSettlement(anyLong());
    }

    @Test
    @DisplayName("최종 정산 지급 성공 시 정산 장부 완료 콜백 호출")
    void processSettlementPayout_success() {
        Settlement settlement = Settlement.builder()
                .ideaId(10L)
                .type(SettlementType.FINAL)
                .totalAmount(100_000L)
                .platformFee(1_000L)
                .payoutAmount(99_000L)
                .idempotencyKey("idea-10-FINAL")
                .build();
        setField(settlement, "id", 2L);

        given(settlementRepository.findById(2L)).willReturn(Optional.of(settlement));
        given(ideaService.getIdea(10L)).willReturn(sampleIdea(10L, 100L));
        given(userRepository.findById(100L)).willReturn(Optional.of(sampleUser(100L)));
        given(paymentProperties.payout()).willReturn(new PaymentProperties.Payout(true, "088", "000", ""));
        given(paymentPayoutService.payout(any(PayoutRequest.class)))
                .willReturn(PayoutResult.success("mock-payout-2"));

        settlementPaymentService.processSettlementPayout(2L, SettlementStatus.COMPLETED);

        ArgumentCaptor<PayoutRequest> requestCaptor = ArgumentCaptor.forClass(PayoutRequest.class);
        verify(paymentPayoutService).payout(requestCaptor.capture());
        PayoutRequest request = requestCaptor.getValue();
        assertThat(request.payoutTargetId()).isEqualTo(2L);
        assertThat(request.payoutTargetType()).isEqualTo(PayoutTargetType.SETTLEMENT);
        assertThat(request.amount()).isEqualTo(99_000L);
        verify(settlementService).completeSettlementPayout(2L, SettlementStatus.COMPLETED);
        verify(settlementService, never()).failSettlementPayout(anyLong());
    }

    @Test
    @DisplayName("보증금 환급 지급 성공 시 보증금 상태를 환급 처리한다")
    void processSettlementPayout_depositRefund_success() {
        Settlement settlement = Settlement.builder()
                .ideaId(10L)
                .type(SettlementType.FINAL)
                .totalAmount(100_000L)
                .platformFee(0L)
                .payoutAmount(100_000L)
                .idempotencyKey("idea-10-DEPOSIT-COMPLETED")
                .build();
        setField(settlement, "id", 3L);

        given(settlementRepository.findById(3L)).willReturn(Optional.of(settlement));
        given(ideaService.getIdea(10L)).willReturn(sampleIdea(10L, 100L));
        given(userRepository.findById(100L)).willReturn(Optional.of(sampleUser(100L)));
        given(paymentProperties.payout()).willReturn(new PaymentProperties.Payout(true, "088", "000", ""));
        given(paymentPayoutService.payout(any(PayoutRequest.class)))
                .willReturn(PayoutResult.success("mock-payout-3"));

        settlementPaymentService.processSettlementPayout(3L, SettlementStatus.DEPOSIT_REFUNDED);

        verify(settlementService).completeSettlementPayout(3L, SettlementStatus.DEPOSIT_REFUNDED);
    }

    @Test
    @DisplayName("정산 지급 실패 시 정산 장부 실패 콜백 호출")
    void processSettlementPayout_failure() {
        Settlement settlement = Settlement.builder()
                .ideaId(10L)
                .type(SettlementType.FINAL)
                .totalAmount(100_000L)
                .platformFee(1_000L)
                .payoutAmount(99_000L)
                .idempotencyKey("idea-10-FINAL")
                .build();
        setField(settlement, "id", 2L);

        given(settlementRepository.findById(2L)).willReturn(Optional.of(settlement));
        given(ideaService.getIdea(10L)).willReturn(sampleIdea(10L, 100L));
        given(userRepository.findById(100L)).willReturn(Optional.of(sampleUser(100L)));
        given(paymentProperties.payout()).willReturn(new PaymentProperties.Payout(true, "088", "000", ""));
        given(paymentPayoutService.payout(any(PayoutRequest.class)))
                .willReturn(PayoutResult.failure("mock-payout-failed"));

        settlementPaymentService.processSettlementPayout(2L, SettlementStatus.COMPLETED);

        verify(settlementService).failSettlementPayout(2L);
        verify(settlementService, never()).completeSettlementPayout(anyLong(), any());
    }

    @Test
    @DisplayName("선정산 지급 이벤트 처리 중 예외 발생 시 FAILED 전환")
    void onPreSettlementPayoutRequested_exception_marksFailed() {
        PreSettlement preSettlement = PreSettlement.builder()
                .ideaId(10L)
                .amount(50_000L)
                .build();
        setField(preSettlement, "id", 1L);

        given(preSettlementRepository.findById(1L)).willReturn(Optional.of(preSettlement));
        given(ideaService.getIdea(10L)).willThrow(new RuntimeException("unexpected"));

        settlementPaymentService.onPreSettlementPayoutRequested(new PreSettlementPayoutRequestedEvent(1L));

        verify(preSettlementService).failPreSettlement(1L);
    }

    @Test
    @DisplayName("정산 지급 이벤트 처리 중 예외 발생 시 FAILED 전환")
    void onSettlementPayoutRequested_exception_marksFailed() {
        Settlement settlement = Settlement.builder()
                .ideaId(10L)
                .type(SettlementType.FINAL)
                .totalAmount(100_000L)
                .platformFee(1_000L)
                .payoutAmount(99_000L)
                .idempotencyKey("idea-10-FINAL")
                .build();
        setField(settlement, "id", 2L);

        given(settlementRepository.findById(2L)).willReturn(Optional.of(settlement));
        given(ideaService.getIdea(10L)).willThrow(new RuntimeException("unexpected"));

        settlementPaymentService.onSettlementPayoutRequested(
                new SettlementPayoutRequestedEvent(2L, SettlementStatus.COMPLETED));

        verify(settlementService).failSettlementPayout(2L);
    }

    @Test
    @DisplayName("PENDING 환불 PG 성공 시 Payment 동기화 및 Refund complete")
    void processRefund_success() {
        Refund refund = Refund.builder()
                .paymentId(5L)
                .sponsorId(200L)
                .amount(10_000L)
                .reason(RefundReason.GOAL_NOT_MET)
                .build();
        setField(refund, "id", 9L);

        Payment payment = Payment.createPending(3L, "order-1", 10_000L, PaymentMethod.CARD);
        setField(payment, "id", 5L);
        payment.complete("pg-key");

        Funding funding = Funding.createPending(10L, 200L, 1, 10_000L, RewardType.REWARD_POINT);
        setField(funding, "id", 3L);
        funding.markAsPaid();

        given(refundRepository.findById(9L)).willReturn(Optional.of(refund));
        given(paymentRepository.findById(5L)).willReturn(Optional.of(payment));
        given(paymentGateway.refund(eq("pg-key"), eq("order-1"), eq(10_000L), any()))
                .willReturn(PaymentRefundResult.success("cancel-1"));
        given(fundingRepository.findByIdForUpdate(3L)).willReturn(Optional.of(funding));

        settlementPaymentService.processRefund(9L);

        verify(refundService).completeRefund(9L);
        verify(paymentGateway).refund(eq("pg-key"), eq("order-1"), eq(10_000L), eq("목표 미달성 환불"));
    }

    private static IdeaResponse sampleIdea(Long ideaId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return new IdeaResponse(
                ideaId,
                userId,
                "title",
                "TECH",
                "intro",
                "problem",
                "solution",
                "goal",
                "target",
                "competitor",
                "team",
                1_000_000L,
                100_000L,
                0L,
                0,
                now,
                now.plusDays(7),
                RewardType.REWARD_POINT.name(),
                null,
                List.of(),
                "IN_PROGRESS",
                null,
                null,
                "NONE",
                now,
                now,
                List.of()
        );
    }

    private static User sampleUser(Long userId) {
        User user = User.create("a@test.com", "pw", "홍길동", "nick", 20, Role.USER);
        setField(user, "id", userId);
        return user;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
