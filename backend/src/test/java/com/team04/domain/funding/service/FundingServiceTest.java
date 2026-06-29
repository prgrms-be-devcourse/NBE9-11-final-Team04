package com.team04.domain.funding.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.payment.service.IdeaVbankPoolService;
import com.team04.domain.payment.service.PaymentService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingServiceTest {

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private IdeaRepository ideaRepository;

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private IdeaVbankPoolService ideaVbankPoolService;

    @InjectMocks
    private FundingService fundingService;

    @Test
    @DisplayName("완료된 마일스톤 단계의 후원은 직접 취소할 수 없다")
    void cancelMySponsorship_lockedMilestone_throwsException() {
        Funding funding = paidFunding(1L, 10L, 1);
        given(fundingRepository.findFirstByIdeaIdAndSponsorIdAndStatusInOrderByCreatedAtDesc(
                1L,
                10L,
                List.of(FundingStatus.PAID, FundingStatus.PENDING_PAYMENT)
        )).willReturn(Optional.of(funding));
        given(fundingRepository.findByIdForUpdate(funding.getId()))
                .willReturn(Optional.of(funding));
        given(milestoneRepository.findMaxStepByIdeaIdAndStatus(1L, MilestoneStatus.COMPLETED))
                .willReturn(1);

        assertThatThrownBy(() -> fundingService.cancelMySponsorship(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FUNDING_LOCKED_BY_MILESTONE);

        verify(paymentService, never()).refundPayment(1L, 10L);
    }

    @Test
    @DisplayName("현재 진행 중인 마일스톤 단계의 후원은 직접 취소할 수 있다")
    void cancelMySponsorship_currentMilestone_refundPayment() {
        Funding funding = paidFunding(1L, 10L, 2);
        Payment payment = successPayment(funding.getId());
        given(fundingRepository.findFirstByIdeaIdAndSponsorIdAndStatusInOrderByCreatedAtDesc(
                1L,
                10L,
                List.of(FundingStatus.PAID, FundingStatus.PENDING_PAYMENT)
        )).willReturn(Optional.of(funding));
        given(fundingRepository.findByIdForUpdate(funding.getId()))
                .willReturn(Optional.of(funding));
        given(milestoneRepository.findMaxStepByIdeaIdAndStatus(1L, MilestoneStatus.COMPLETED))
                .willReturn(1);
        given(paymentRepository.findFirstByFundingIdAndStatusForUpdate(
                funding.getId(), PaymentStatus.SUCCESS.name()))
                .willReturn(Optional.of(payment));

        fundingService.cancelMySponsorship(1L, 10L);

        verify(paymentService).refundPayment(payment.getId(), 10L);
    }

    private Funding paidFunding(Long ideaId, Long sponsorId, int milestoneStep) {
        Funding funding = Funding.createPending(
                ideaId,
                sponsorId,
                milestoneStep,
                10000L,
                RewardType.REWARD_POINT
        );
        ReflectionTestUtils.setField(funding, "id", 100L + milestoneStep);
        ReflectionTestUtils.setField(funding, "status", FundingStatus.PAID);
        return funding;
    }

    private Payment successPayment(Long fundingId) {
        Payment payment = Payment.createPending(
                fundingId,
                "order-" + fundingId,
                10000L,
                PaymentMethod.CARD
        );
        ReflectionTestUtils.setField(payment, "id", 200L + fundingId);
        ReflectionTestUtils.setField(payment, "status", PaymentStatus.SUCCESS);
        return payment;
    }
}
