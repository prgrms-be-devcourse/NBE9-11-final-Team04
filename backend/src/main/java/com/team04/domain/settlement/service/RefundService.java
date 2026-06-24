package com.team04.domain.settlement.service;

import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final FundingRepository fundingRepository;
    private final PaymentRepository paymentRepository;
    private final PreSettlementRepository preSettlementRepository;
    private final DepositRepository depositRepository;
    private final IdeaService ideaService;

    /**
     * 목표 미달성 환불 레코드 일괄 생성
     * 후원금 전액 환불 — reason 고정 (GOAL_NOT_MET)
     * SettlementScheduler에서 호출
     */
    @Transactional
    public void createGoalNotMetRefunds(Long ideaId) {
        List<Object[]> results = paymentRepository.findPaymentsAndSponsorIdsToRefund(ideaId);

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
     *   TODO: 정욱님 잔액 추적 PR 머지 후 계산 로직 교체
     *
     * TODO: 정욱님 — Deposit.forfeit() / release() 호출 주체 확정 후 수정
     */
    @Transactional
    public void createCancelRefunds(Long ideaId, boolean isJustified) {
        List<Object[]> results = paymentRepository.findPaymentsAndSponsorIdsToRefund(ideaId);
        if (results.isEmpty()) return;

        // 보증금 조회
        // TODO: 정욱님 — Deposit.forfeit() / release() 호출 주체 확정 후 수정
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

        // 후원금 잔액을 후원 비율대로 분배
        long finalFundingBalance = fundingBalance;
        List<Refund> refunds = results.stream()
                .map(row -> {
                    Payment payment = (Payment) row[0];
                    Long sponsorId = (Long) row[1];
                    // 후원 비율 = 개인 후원금 / 총 후원금
                    long refundAmount = totalPayment > 0
                            ? Math.round((double) payment.getAmount() / totalPayment * finalFundingBalance)
                            : 0;
                    return Refund.builder()
                            .paymentId(payment.getId())
                            .sponsorId(sponsorId)
                            .amount(refundAmount)
                            .reason(RefundReason.CANCELLED)
                            .build();
                })
                .toList();

        // 단순 포기/먹튀: 보증금 전액을 후원자 균등 분배하여 추가
        if (!isJustified) {
            long depositPerSponsor = depositAmount / results.size();
            refunds = refunds.stream()
                    .map(refund -> Refund.builder()
                            .paymentId(refund.getPaymentId())
                            .sponsorId(refund.getSponsorId())
                            .amount(refund.getAmount() + depositPerSponsor)
                            .reason(RefundReason.CANCELLED)
                            .build())
                    .toList();

            // TODO: 정욱님 — Deposit.forfeit() / release() 호출 주체 확정 후 수정
            deposit.forfeit();
        } else {
            // 정당한 사유: 보증금 - 선정산 잔액이 있으면 제안자에게 환급
            // TODO: 정욱님 — Deposit.forfeit() / release() 호출 주체 확정 후 수정
            if (preSettlementTotal < depositAmount) {
                deposit.release();
            } else {
                deposit.forfeit();
            }
        }

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
    public RefundResponse createDisputeRefund(Long paymentId) {
        if (refundRepository.existsByPaymentId(paymentId)) {
            throw new CustomException(ErrorCode.REFUND_ALREADY_COMPLETED);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        com.team04.domain.funding.entity.Funding funding = fundingRepository.findById(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .sponsorId(funding.getSponsorId())
                .amount(payment.getAmount())
                .reason(RefundReason.DISPUTE)
                .build();

        return RefundResponse.from(refundRepository.save(refund));
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
     * 후원자별 환불 내역 조회
     */
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsBySponsor(Long sponsorId) {
        return refundRepository.findBySponsorId(sponsorId)
                .stream()
                .map(RefundResponse::from)
                .toList();
    }
}