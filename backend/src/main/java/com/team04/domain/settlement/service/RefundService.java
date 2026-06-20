package com.team04.domain.settlement.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.settlement.dto.response.RefundResponse;
import com.team04.domain.settlement.entity.Refund;
import com.team04.domain.settlement.entity.RefundReason;
import com.team04.domain.settlement.repository.RefundRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final FundingRepository fundingRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 목표 미달성 환불 레코드 일괄 생성
     * SettlementScheduler에서 호출 — reason 고정 (GOAL_NOT_MET)
     *
     * TODO: FundingRepository에 findAllByIdeaId(Long ideaId) 추가 요청 필요
     *       현재 Pageable.unpaged() 임시 처리 — 데이터 누락 위험 있음
     * TODO: Payment를 Funding별로 개별 조회하는 N+1 문제 존재
     *       findAllByIdeaId() 추가 시 조인 쿼리로 개선 필요
     */
    @Transactional
    public void createGoalNotMetRefunds(Long ideaId) {
        createRefundsForIdea(ideaId, RefundReason.GOAL_NOT_MET);
    }

    /**
     * 이행 중단 환불 레코드 일괄 생성
     * MilestoneService.cancelMilestone()에서 호출 — reason 고정 (CANCELLED)
     *
     * TODO: FundingRepository에 findAllByIdeaId(Long ideaId) 추가 요청 필요
     *       현재 Pageable.unpaged() 임시 처리 — 데이터 누락 위험 있음
     * TODO: Payment를 Funding별로 개별 조회하는 N+1 문제 존재
     *       findAllByIdeaId() 추가 시 조인 쿼리로 개선 필요
     */
    @Transactional
    public void createCancelRefunds(Long ideaId) {
        createRefundsForIdea(ideaId, RefundReason.CANCELLED);
    }

    /**
     * 분쟁 환불 레코드 생성 (관리자 수동 처리, 단건)
     * sponsorId는 payment → funding 흐름으로 내부 조회 (오입력 방지)
     * 환불 금액은 실제 결제 금액(payment.getAmount())으로 고정 (과다 환불 방지)
     */
    @Transactional
    public RefundResponse createDisputeRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        Funding funding = fundingRepository.findById(payment.getFundingId())
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

    /**
     * ideaId 기준으로 모든 후원자의 환불 레코드를 일괄 생성합니다.
     * Funding → Payment 순으로 조회하여 후원자별 Refund를 생성합니다.
     * saveAll()로 일괄 저장하여 DB 쓰기 횟수를 최소화합니다.
     * paymentId unique 제약으로 DB 레벨에서 중복 환불이 방지됩니다.
     *
     * TODO: fundingRepository.findAllByIdeaId(ideaId) 추가 후 변경 필요
     *       현재 Pageable.unpaged() 임시 처리 — 데이터 누락 위험 있음
     */
    private void createRefundsForIdea(Long ideaId, RefundReason reason) {
        List<Funding> fundings = fundingRepository
                .findByIdeaIdOrderByCreatedAtDesc(ideaId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        List<Refund> refundsToSave = new ArrayList<>();
        for (Funding funding : fundings) {
            List<Payment> payments = paymentRepository.findByFundingIdOrderByCreatedAtDesc(funding.getId());

            payments.stream()
                    .filter(p -> p.getStatus() == PaymentTypes.PaymentStatus.SUCCESS)
                    .forEach(payment -> refundsToSave.add(
                            Refund.builder()
                                    .paymentId(payment.getId())
                                    .sponsorId(funding.getSponsorId())
                                    .amount(payment.getAmount())
                                    .reason(reason)
                                    .build()
                    ));
        }

        if (!refundsToSave.isEmpty()) {
            refundRepository.saveAll(refundsToSave);
        }
    }
}