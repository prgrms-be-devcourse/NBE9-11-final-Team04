package com.team04.domain.settlement.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.dto.response.PaymentRefundResult;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundReason;
import com.team04.domain.settlement.entity.RefundStatus;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import com.team04.domain.settlement.repository.RefundRepository;
import com.team04.domain.settlement.repository.SettlementRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 환불 5단계 오케스트레이션:
 * 1. Refund PENDING 생성
 * 2. PG 환불 API 호출
 * 3. PG 결과/웹훅 수신
 * 4. Refund COMPLETED/FAILED 반영
 * 5. Payment/Funding/Idea/Settlement 동기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final FundingRepository fundingRepository;
    private final PaymentRepository paymentRepository;
    private final IdeaRepository ideaRepository;
    private final SettlementRepository settlementRepository;
    private final PaymentGateway paymentGateway;
    private final ObjectProvider<RefundService> selfProvider;

    /**
     * 목표 미달성 환불 레코드 일괄 생성 후 PG 실행 대상 ID 반환
     */
    @Transactional
    public List<Long> createGoalNotMetRefunds(Long ideaId) {
        return createRefundsForIdea(ideaId, RefundReason.GOAL_NOT_MET);
    }

    /**
     * 이행 중단 환불 레코드 일괄 생성 후 PG 실행 대상 ID 반환
     */
    @Transactional
    public List<Long> createCancelRefunds(Long ideaId) {
        return createRefundsForIdea(ideaId, RefundReason.CANCELLED);
    }

    /**
     * 후원자 자발 취소 — PENDING 생성 후 즉시 PG 환불 실행
     */
    public RefundResponse requestSponsorRefund(Long paymentId, Long sponsorId) {
        Refund refund = selfProvider.getObject().createPendingRefund(paymentId, RefundReason.SPONSOR_CANCEL, sponsorId);
        executeRefund(refund.getId());
        return RefundResponse.from(refundRepository.findById(refund.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND)));
    }

    /**
     * 분쟁 환불 — PENDING 생성 후 PG 환불 실행
     */
    public RefundResponse createDisputeRefund(Long paymentId) {
        Refund refund = selfProvider.getObject().createPendingRefund(paymentId, RefundReason.DISPUTE, null);
        executeRefund(refund.getId());
        return RefundResponse.from(refundRepository.findById(refund.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND)));
    }

    /**
     * PG 환불 API 호출 (2단계) — 트랜잭션 밖에서 실행
     */
    public void executeRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getStatus() != RefundStatus.PENDING) {
            return;
        }

        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getPaymentKey() == null || payment.getPaymentKey().isBlank()) {
            selfProvider.getObject().failRefund(refundId, "PG 결제 키가 없어 환불할 수 없습니다");
            return;
        }

        PaymentRefundResult result = paymentGateway.refund(
                payment.getPaymentKey(),
                payment.getOrderId(),
                refund.getAmount(),
                refund.getReason().name()
        );

        if (result.success()) {
            selfProvider.getObject().completeRefundWithSync(refundId, result.pgCancelKey());
        } else {
            selfProvider.getObject().failRefund(refundId, result.failureMessage());
        }
    }

    /**
     * PG 웹훅/콜백으로 환불 완료 처리 (3~5단계)
     */
    @Transactional
    public RefundResponse completeRefundWithSync(Long refundId, String pgCancelKey) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));

        if (refund.getStatus() == RefundStatus.COMPLETED) {
            return RefundResponse.from(refund);
        }
        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new CustomException(ErrorCode.REFUND_NOT_PENDING);
        }

        refund.complete(pgCancelKey);
        syncPaymentFunding(refund);
        tryFinalizeRefundSettlement(refund.getIdeaId());
        return RefundResponse.from(refund);
    }

    /**
     * PG 환불 실패 반영 (4단계)
     */
    @Transactional
    public RefundResponse failRefund(Long refundId, String failureReason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));
        refund.fail(failureReason);
        return RefundResponse.from(refund);
    }

    /**
     * 실패한 환불 재시도
     */
    public RefundResponse retryRefund(Long refundId) {
        Refund refund = selfProvider.getObject().markRefundRetryable(refundId);
        executeRefund(refund.getId());
        return RefundResponse.from(refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND)));
    }

    /**
     * 관리자 수동 완료 (PG 콜백 대체)
     */
    @Transactional
    public RefundResponse completeRefund(Long refundId) {
        return completeRefundWithSync(refundId, "manual-complete");
    }

    /**
     * orderId 기준 웹훅 환불 완료 처리
     */
    @Transactional
    public void completeRefundByOrderId(String orderId, String pgCancelKey) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        Refund refund = refundRepository.findFirstByPaymentId(payment.getId())
                .orElse(null);
        if (refund == null || refund.getStatus() != RefundStatus.PENDING) {
            return;
        }
        completeRefundWithSync(refund.getId(), pgCancelKey != null ? pgCancelKey : "webhook-cancel");
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsBySponsor(Long sponsorId) {
        return refundRepository.findBySponsorId(sponsorId)
                .stream()
                .map(RefundResponse::from)
                .toList();
    }

    /**
     * 프로젝트 단위 환불 Settlement 확정 — 모든 Refund COMPLETED 시 REFUNDED 전환
     */
    @Transactional
    public void tryFinalizeRefundSettlement(Long ideaId) {
        Settlement settlement = settlementRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId).stream()
                .filter(s -> s.getStatus() == SettlementStatus.PENDING)
                .filter(s -> s.getIdempotencyKey().contains("-REFUND-"))
                .findFirst()
                .orElse(null);

        if (settlement == null) {
            return;
        }

        if (refundRepository.existsByIdeaIdAndStatus(ideaId, RefundStatus.PENDING)) {
            return;
        }
        if (refundRepository.existsByIdeaIdAndStatus(ideaId, RefundStatus.FAILED)) {
            return;
        }

        List<Payment> successPayments = findSuccessPaymentsForIdea(ideaId);
        if (successPayments.isEmpty()) {
            settlement.refund();
            return;
        }

        boolean allRefunded = successPayments.stream()
                .allMatch(payment -> refundRepository.findFirstByPaymentId(payment.getId())
                        .map(refund -> refund.getStatus() == RefundStatus.COMPLETED)
                        .orElse(false));

        if (allRefunded) {
            settlement.refund();
        }
    }

    @Transactional
    protected Refund markRefundRetryable(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));
        refund.retry();
        return refund;
    }

    @Transactional
    protected Refund createPendingRefund(Long paymentId, RefundReason reason, Long expectedSponsorId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        Funding funding = fundingRepository.findById(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        if (expectedSponsorId != null && !funding.getSponsorId().equals(expectedSponsorId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (funding.getStatus() != FundingStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        Refund existing = refundRepository.findFirstByPaymentId(paymentId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == RefundStatus.COMPLETED) {
                throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
            }
            if (existing.getStatus() == RefundStatus.FAILED) {
                existing.retry();
            }
            return existing;
        }

        Refund refund = Refund.builder()
                .ideaId(funding.getIdeaId())
                .paymentId(payment.getId())
                .sponsorId(funding.getSponsorId())
                .amount(payment.getAmount())
                .reason(reason)
                .build();

        return refundRepository.save(refund);
    }

    private void syncPaymentFunding(Refund refund) {
        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        Funding funding = fundingRepository.findByIdForUpdate(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        payment.markAsRefunded();
        funding.markAsRefunded();

        ideaRepository.findByIdForUpdate(funding.getIdeaId())
                .ifPresent(idea -> idea.subtractFundingAmount(funding.getAmount()));
    }

    private List<Long> createRefundsForIdea(Long ideaId, RefundReason reason) {
        List<Funding> fundings = fundingRepository
                .findByIdeaIdOrderByCreatedAtDesc(ideaId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        List<Long> refundIds = new ArrayList<>();
        for (Funding funding : fundings) {
            List<Payment> payments = paymentRepository.findByFundingIdOrderByCreatedAtDesc(funding.getId());

            for (Payment payment : payments) {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    continue;
                }
                if (refundRepository.existsByPaymentId(payment.getId())) {
                    refundRepository.findFirstByPaymentId(payment.getId())
                            .filter(refund -> refund.getStatus() == RefundStatus.PENDING
                                    || refund.getStatus() == RefundStatus.FAILED)
                            .ifPresent(refund -> refundIds.add(refund.getId()));
                    continue;
                }

                Refund refund = refundRepository.save(Refund.builder()
                        .ideaId(ideaId)
                        .paymentId(payment.getId())
                        .sponsorId(funding.getSponsorId())
                        .amount(payment.getAmount())
                        .reason(reason)
                        .build());
                refundIds.add(refund.getId());
            }
        }
        return refundIds;
    }

    private List<Payment> findSuccessPaymentsForIdea(Long ideaId) {
        List<Funding> fundings = fundingRepository
                .findByIdeaIdOrderByCreatedAtDesc(ideaId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        List<Payment> payments = new ArrayList<>();
        for (Funding funding : fundings) {
            paymentRepository.findByFundingIdOrderByCreatedAtDesc(funding.getId()).stream()
                    .filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS)
                    .forEach(payments::add);
        }
        return payments;
    }
}
