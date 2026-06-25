package com.team04.domain.settlement.service;

import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.entity.TargetType;
import com.team04.domain.dispute.repository.DisputeRepository;
import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundReason;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.RefundRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final FundingRepository fundingRepository;
    private final PaymentRepository paymentRepository;
    private final PreSettlementRepository preSettlementRepository;
    private final DepositRepository depositRepository;
    private final IdeaService ideaService;
    private final FundingService fundingService;
    private final DisputeRepository disputeRepository;

    /**
     * 목표 미달성 환불 레코드 일괄 생성
     * 후원금 전액 환불 — reason 고정 (GOAL_NOT_MET)
     * SettlementScheduler에서 호출
     */
    @Transactional
    public void createGoalNotMetRefunds(Long ideaId) {
        List<Object[]> results = excludeAlreadyRefundedPayments(paymentRepository.findPaymentsAndSponsorIdsToRefund(ideaId));

        List<Refund> refunds = results.stream()
                .map(row -> {
                    Payment payment = (Payment) row[0];
                    Long sponsorId = (Long) row[1];
                    return Refund.builder()
                            .paymentId(payment.getId())
                            .sponsorId(sponsorId)
                            .amount(payment.getAmount())
                            .reason(RefundReason.GOAL_NOT_MET)
                            .build();
                })
                .toList();

        if (!refunds.isEmpty()) {
            refundRepository.saveAll(refunds);
        }
    }

    /**
     * 이행 중단 환불 레코드 일괄 생성 (정당한 사유 / 단순 포기 / 먹튀)
     * MilestoneService.cancelMilestone() / refundMilestone()에서 호출
     *
     * 환불 재원:
     *   - 후원금 잔액 (가상계좌 잔액 - 보증금) 을 후원 비율대로 분배
     *   - 단순 포기/먹튀(CANCELLED): 보증금 전액 추가하여 균등 분배
     *   - 정당한 사유(CANCELLED_JUSTIFIED): 후원금 잔액만 분배
     *
     * 가상계좌 잔액 계산:
     *   SUM(payment SUCCESS) + 보증금 - SUM(pre_settlement COMPLETED)
     *
     * Deposit 상태 변경은 FundingService.forfeitDeposit() / releaseDeposit()으로 처리
     */
    @Transactional
    public void createCancelRefunds(Long ideaId, boolean isJustified) {
        List<Object[]> results = excludeAlreadyRefundedPayments(paymentRepository.findPaymentsAndSponsorIdsToRefund(ideaId));
        if (results.isEmpty()) {
            updateDepositStatus(ideaId, isJustified);
            return;
        }

        // 보증금 조회
        Deposit deposit = depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEPOSIT_NOT_FOUND));
        long depositAmount = deposit.getAmount();

        // 선정산 완료액 조회
        long preSettlementTotal = preSettlementRepository
                .sumAmountByIdeaIdAndStatus(ideaId, PreSettlementStatus.COMPLETED);

        // 총 후원금
        long totalPayment = results.stream()
                .mapToLong(row -> ((Payment) row[0]).getAmount())
                .sum();

        // 후원금 잔액 = 총 후원금 - 선정산액
        long fundingBalance = Math.max(totalPayment - preSettlementTotal, 0);

        List<Refund> refunds = distributeFundingBalance(results, totalPayment, fundingBalance);

        // 단순 포기/먹튀: 보증금 전액을 후원자 균등 분배하여 추가
        if (!isJustified) {
            refunds = addDepositShare(refunds, depositAmount);
        } else {
            // 정당한 사유: 선정산 여부와 관계없이 release (보증금 소진이어도 몰수가 아님)
        }
        updateDepositStatus(ideaId, isJustified);

        if (!refunds.isEmpty()) {
            refundRepository.saveAll(refunds);
        }
    }

    /**
     * 분쟁 환불 레코드 생성 (관리자 수동 처리, 단건)
     * sponsorId는 payment → funding 흐름으로 내부 조회 (오입력 방지)
     * 환불 금액은 실제 결제 금액으로 고정 (과다 환불 방지)
     * 이미 환불된 결제건이면 비즈니스 예외 발생
     *
     * TODO: 경탁님 — DisputeResolvedEvent 인터페이스 확정 후 전체 환불 여부 결정
     */
    @Transactional
    public RefundResponse createDisputeRefund(Long paymentId, Long ideaId) {
        if (refundRepository.existsByPaymentId(paymentId)) {
            throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        com.team04.domain.funding.entity.Funding funding = fundingRepository.findById(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        if (!funding.getIdeaId().equals(ideaId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .sponsorId(funding.getSponsorId())
                .amount(payment.getAmount())
                .reason(RefundReason.DISPUTE)
                .build();

        return RefundResponse.from(refundRepository.save(refund));
    }

    /**
     * 분쟁 단건 환불
     * RESOLVED 상태의 IDEA 분쟁에서 관리자가 특정 후원자 결제를 수동 환불할 때 사용
     * 전체 환불(DisputeResolvedEvent)로 처리되지 못한 건을 개별 재처리하는 용도
     */
    @Transactional
    public RefundResponse forceDisputeRefund(Long disputeId, Long paymentId) {
        var dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new CustomException(ErrorCode.DISPUTE_NOT_FOUND));
        if (dispute.getStatus() != DisputeStatus.RESOLVED) {
            throw new CustomException(ErrorCode.DISPUTE_INVALID_STATUS_TRANSITION);
        }
        if (dispute.getTargetType() != TargetType.IDEA) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return createDisputeRefund(paymentId, dispute.getTargetId());
    }

    /**
     * 환불 완료 처리
     * 결제팀 콜백용 — PENDING → COMPLETED
     */
    @Transactional
    public RefundResponse completeRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));
        refund.complete();
        return RefundResponse.from(refund);
    }

    /**
     * 환불 실패 처리
     * 결제팀 콜백용 — PENDING → FAILED
     */
    @Transactional
    public RefundResponse failRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new CustomException(ErrorCode.REFUND_NOT_FOUND));
        refund.fail();
        return RefundResponse.from(refund);
    }

    /**
     * 후원자별 환불 내역 조회
     */
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsBySponsor(Long sponsorId) {
        return refundRepository.findBySponsorId(sponsorId)
                .stream()
                .map(RefundResponse::from)
                .toList();
    }

    // 이미 환불 레코드가 생성된 결제건은 재환불 대상에서 제외한다.
    // force refund는 아직 환불되지 않은 후원자들에게 남은 환불 가능 금액을 재분배하는 정책이다.
    // 따라서 totalPayment, fundingBalance, depositShare 계산은 환불 대상자 기준으로 다시 수행한다.
    private List<Object[]> excludeAlreadyRefundedPayments(List<Object[]> results) {
        if (results.isEmpty()) {
            return results;
        }

        Set<Long> paymentIds = results.stream()
                .map(row -> ((Payment) row[0]).getId())
                .collect(Collectors.toSet());

        Set<Long> refundedPaymentIds = refundRepository.findByPaymentIdIn(paymentIds)
                .stream()
                .map(Refund::getPaymentId)
                .collect(Collectors.toSet());

        if (refundedPaymentIds.isEmpty()) {
            return results;
        }

        return results.stream()
                .filter(row -> !refundedPaymentIds.contains(((Payment) row[0]).getId()))
                .toList();
    }

    private void updateDepositStatus(Long ideaId, boolean isJustified) {
        if (isJustified) {
            fundingService.releaseDeposit(ideaId);
            return;
        }
        fundingService.forfeitDeposit(ideaId);
    }

    private List<Refund> addDepositShare(List<Refund> refunds, long depositAmount) {
        long depositPerSponsor = depositAmount / refunds.size();
        long remainder = depositAmount % refunds.size();
        List<Refund> sortedRefunds = refunds.stream()
                .sorted((left, right) -> left.getPaymentId().compareTo(right.getPaymentId()))
                .toList();

        return IntStream.range(0, sortedRefunds.size())
                .mapToObj(index -> {
                    Refund refund = sortedRefunds.get(index);
                    long remainderShare = index < remainder ? 1 : 0;
                    return Refund.builder()
                            .paymentId(refund.getPaymentId())
                            .sponsorId(refund.getSponsorId())
                            .amount(refund.getAmount() + depositPerSponsor + remainderShare)
                            .reason(RefundReason.CANCELLED)
                            .build();
                })
                .toList();
    }

    private List<Refund> distributeFundingBalance(List<Object[]> results, long totalPayment, long fundingBalance) {
        if (totalPayment <= 0 || fundingBalance <= 0) {
            return results.stream()
                    .map(row -> {
                        Payment payment = (Payment) row[0];
                        Long sponsorId = (Long) row[1];
                        return createCancelRefund(payment, sponsorId, 0);
                    })
                    .toList();
        }

        List<FundingShare> shares = results.stream()
                .map(row -> {
                    Payment payment = (Payment) row[0];
                    Long sponsorId = (Long) row[1];
                    long weightedAmount = payment.getAmount() * fundingBalance;
                    long baseAmount = weightedAmount / totalPayment;
                    long remainder = weightedAmount % totalPayment;
                    return new FundingShare(payment, sponsorId, baseAmount, remainder);
                })
                .toList();

        long distributedAmount = shares.stream()
                .mapToLong(FundingShare::baseAmount)
                .sum();
        long remainderAmount = fundingBalance - distributedAmount;
        Set<Long> extraPaymentIds = shares.stream()
                .sorted(Comparator
                        .comparingLong(FundingShare::remainder).reversed()
                        .thenComparing(share -> share.payment().getId()))
                .limit(remainderAmount)
                .map(share -> share.payment().getId())
                .collect(Collectors.toSet());

        return shares.stream()
                .sorted(Comparator.comparing(share -> share.payment().getId()))
                .map(share -> createCancelRefund(
                        share.payment(),
                        share.sponsorId(),
                        share.baseAmount() + (extraPaymentIds.contains(share.payment().getId()) ? 1 : 0)
                ))
                .toList();
    }

    private Refund createCancelRefund(Payment payment, Long sponsorId, long amount) {
        return Refund.builder()
                .paymentId(payment.getId())
                .sponsorId(sponsorId)
                .amount(amount)
                .reason(RefundReason.CANCELLED)
                .build();
    }

    private record FundingShare(Payment payment, Long sponsorId, long baseAmount, long remainder) {
    }
}
