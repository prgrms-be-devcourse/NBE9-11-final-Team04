package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.client.PaymentGateway;
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
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.RefundRepository;
import com.team04.domain.settlement.service.PreSettlementService;
import com.team04.domain.settlement.service.RefundService;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.config.payment.PaymentProperties;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
    private final PaymentProperties paymentProperties;
    private final PaymentRepository paymentRepository;
    private final FundingRepository fundingRepository;
    private final IdeaRepository ideaRepository;
    private final IdeaService ideaService;
    private final UserRepository userRepository;
    private final RefundRepository refundRepository;
    private final PreSettlementRepository preSettlementRepository;
    @Lazy
    private final PreSettlementService preSettlementService;
    @Lazy
    private final RefundService refundService;

    /** 선정산 REQUESTED 건에 대해 지급대행을 요청하고 결과에 따라 complete/fail 처리 */
    public void processPreSettlementPayout(Long preSettlementId) {
        PreSettlement preSettlement = preSettlementRepository.findById(preSettlementId).orElse(null);
        if (preSettlement == null || preSettlement.getStatus() != PreSettlementStatus.REQUESTED) {
            return;
        }

        PayoutRequest request = buildPayoutRequest(preSettlement);
        PayoutResult result = paymentGateway.payout(request);

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

    @Transactional(readOnly = true)
    public List<Refund> findPendingRefunds() {
        return refundRepository.findByStatus(RefundStatus.PENDING);
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

    private PayoutRequest buildPayoutRequest(PreSettlement preSettlement) {
        IdeaResponse idea = ideaService.getIdea(preSettlement.getIdeaId());
        User proposer = userRepository.findById(idea.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        PaymentProperties.Payout payoutConfig = paymentProperties.payout();
        return new PayoutRequest(
                preSettlement.getId(),
                preSettlement.getIdeaId(),
                preSettlement.getAmount(),
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
