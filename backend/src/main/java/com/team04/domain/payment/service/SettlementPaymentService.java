package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.event.PreSettlementPayoutRequestedEvent;
import com.team04.domain.payment.event.SettlementPayoutRequestedEvent;
import com.team04.domain.payment.dto.request.PayoutRequest;
import com.team04.domain.payment.dto.response.PaymentRefundResult;
import com.team04.domain.payment.dto.response.PayoutResult;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundReason;
import com.team04.domain.settlement.entity.RefundStatus;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.RefundRepository;
import com.team04.domain.settlement.repository.SettlementRepository;
import com.team04.domain.settlement.service.PreSettlementService;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.settlement.service.SettlementService;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.config.payment.PaymentProperties;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 정산 장부와 PG 연동을 잇는 최소 오케스트레이터.
 * 선정산 지급 요청·환불 실행 후 정산 도메인 complete/fail 콜백을 호출합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementPaymentService {

    private final PaymentGateway paymentGateway;
    private final PaymentPayoutService paymentPayoutService;
    private final PaymentProperties paymentProperties;
    private final PaymentRepository paymentRepository;
    private final FundingRepository fundingRepository;
    private final IdeaRepository ideaRepository;
    private final IdeaService ideaService;
    private final UserRepository userRepository;
    private final RefundRepository refundRepository;
    private final PreSettlementRepository preSettlementRepository;
    private final SettlementRepository settlementRepository;
    private final PreSettlementService preSettlementService;
    private final RefundService refundService;
    private final SettlementService settlementService;

    @EventListener
    public void onPreSettlementPayoutRequested(PreSettlementPayoutRequestedEvent event) {
        try {
            processPreSettlementPayout(event.preSettlementId());
        } catch (Exception e) {
            log.error("선정산 지급 처리 실패 - preSettlementId: {}, error: {}",
                    event.preSettlementId(), e.getMessage(), e);
        }
    }

    @EventListener
    public void onSettlementPayoutRequested(SettlementPayoutRequestedEvent event) {
        try {
            processSettlementPayout(event.settlementId(), event.successStatus());
        } catch (Exception e) {
            log.error("정산 지급 처리 실패 - settlementId: {}, error: {}",
                    event.settlementId(), e.getMessage(), e);
        }
    }

    /** 선정산 REQUESTED 건에 대해 지급대행을 요청하고 결과에 따라 complete/fail 처리 */
    public void processPreSettlementPayout(Long preSettlementId) {
        PreSettlement preSettlement = preSettlementRepository.findById(preSettlementId).orElse(null);
        if (preSettlement == null || preSettlement.getStatus() != PreSettlementStatus.REQUESTED) {
            return;
        }

        PayoutRequest request = buildPreSettlementPayoutRequest(preSettlement);
        PayoutResult result = paymentPayoutService.payout(request);

        if (result.success() && shouldAutoCompletePayout(result)) {
            preSettlementService.completePreSettlement(preSettlementId);
            return;
        }
        if (!result.success()) {
            log.error("선정산 지급 실패 preSettlementId={}, message={}",
                    preSettlementId, result.failureMessage());
            preSettlementService.failPreSettlement(preSettlementId);
        }
    }

    /** 최종 정산/보증금 환급 PENDING 건에 대해 지급대행을 요청하고 결과에 따라 상태를 전환 */
    public void processSettlementPayout(Long settlementId, SettlementStatus successStatus) {
        Settlement settlement = settlementRepository.findById(settlementId).orElse(null);
        if (settlement == null || settlement.getStatus() != SettlementStatus.PENDING) {
            return;
        }
        if (settlement.getPayoutAmount() <= 0) {
            settlementService.completeSettlementPayout(settlementId, successStatus);
            return;
        }

        PayoutRequest request = buildSettlementPayoutRequest(settlement);
        PayoutResult result = paymentPayoutService.payout(request);

        if (result.success() && shouldAutoCompletePayout(result)) {
            settlementService.completeSettlementPayout(settlementId, successStatus);
            return;
        }
        if (!result.success()) {
            log.error("정산 지급 실패 settlementId={}, message={}",
                    settlementId, result.failureMessage());
            settlementService.failSettlementPayout(settlementId);
        }
    }

    @Transactional(readOnly = true)
    public List<Refund> findPendingRefunds() {
        return refundRepository.findByStatus(RefundStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<PreSettlement> findFailedPreSettlements() {
        return preSettlementRepository.findByStatus(PreSettlementStatus.FAILED);
    }

    @Transactional(readOnly = true)
    public List<Settlement> findFailedSettlements() {
        return settlementRepository.findByStatus(SettlementStatus.FAILED);
    }

    public void retryPreSettlementPayout(Long preSettlementId) {
        // FAILED 상태는 payout 처리 대상이 아니므로 REQUESTED로 되돌린 뒤 재처리한다.
        preSettlementService.retryPreSettlementPayout(preSettlementId);
        processPreSettlementPayout(preSettlementId);
    }

    public void retrySettlementPayout(Long settlementId, SettlementStatus successStatus) {
        // FAILED 상태는 payout 처리 대상이 아니므로 PENDING으로 되돌린 뒤 원래 성공 상태로 재처리한다.
        settlementService.retrySettlementPayout(settlementId);
        processSettlementPayout(settlementId, successStatus);
    }

    /** PENDING 환불 건을 PG 환불 후 Payment/Funding 동기화 및 Refund complete */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getStatus() != RefundStatus.PENDING) {
            return;
        }

        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            refundService.completeRefund(refundId);
            return;
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("환불 대상 결제 상태가 SUCCESS가 아님 refundId={}, paymentId={}, status={}",
                    refundId, payment.getId(), payment.getStatus());
            return;
        }

        PaymentRefundResult refundResult = paymentGateway.refund(
                payment.getPaymentKey(),
                payment.getOrderId(),
                refund.getAmount(),
                resolveCancelReason(refund.getReason())
        );

        if (!refundResult.success()) {
            log.error("정산 환불 PG 실패 refundId={}, paymentId={}", refundId, payment.getId());
            return;
        }

        syncPaymentRefunded(payment);
        refundService.completeRefund(refundId);
    }

    private PayoutRequest buildPreSettlementPayoutRequest(PreSettlement preSettlement) {
        IdeaResponse idea = ideaService.getIdea(preSettlement.getIdeaId());
        User proposer = userRepository.findById(idea.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        PaymentProperties.Payout payoutConfig = paymentProperties.payout();
        return PayoutRequest.preSettlement(
                preSettlement.getId(),
                preSettlement.getIdeaId(),
                preSettlement.getAmount(),
                proposer.getName(),
                payoutConfig.bankCode(),
                payoutConfig.accountNumber()
        );
    }

    private PayoutRequest buildSettlementPayoutRequest(Settlement settlement) {
        IdeaResponse idea = ideaService.getIdea(settlement.getIdeaId());
        User proposer = userRepository.findById(idea.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        PaymentProperties.Payout payoutConfig = paymentProperties.payout();
        return PayoutRequest.settlement(
                settlement.getId(),
                settlement.getIdeaId(),
                settlement.getPayoutAmount(),
                proposer.getName(),
                payoutConfig.bankCode(),
                payoutConfig.accountNumber()
        );
    }

    private boolean shouldAutoCompletePayout(PayoutResult result) {
        if (result.payoutId() != null && result.payoutId().startsWith("skipped:")) {
            return paymentProperties.payout().autoComplete();
        }
        return true;
    }

    private void syncPaymentRefunded(Payment payment) {
        Funding funding = fundingRepository.findByIdForUpdate(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        funding.markAsRefunded();
        payment.markAsRefunded();

        ideaRepository.findByIdForUpdate(funding.getIdeaId())
                .ifPresent(idea -> idea.subtractFundingAmount(funding.getAmount()));
    }

    private String resolveCancelReason(RefundReason reason) {
        return switch (reason) {
            case GOAL_NOT_MET -> "목표 미달성 환불";
            case CANCELLED -> "이행 중단 환불";
            case DISPUTE -> "분쟁 환불";
        };
    }
}
