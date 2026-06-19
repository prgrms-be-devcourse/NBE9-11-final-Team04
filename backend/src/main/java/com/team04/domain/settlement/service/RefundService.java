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
     * 후원자별로 Refund 레코드를 각각 생성합니다.
     *
     * TODO: FundingRepository에 findAllByIdeaId(Long ideaId) 추가 요청 필요
     *       현재 페이징 방식만 존재하여 전체 조회 불가
     */
    @Transactional
    public void createGoalNotMetRefunds(Long ideaId) {
        createRefundsForIdea(ideaId, RefundReason.GOAL_NOT_MET);
    }

    /**
     * 이행 중단 환불 레코드 일괄 생성
     * MilestoneService.cancelMilestone()에서 호출 — reason 고정 (CANCELLED)
     * 후원자별로 Refund 레코드를 각각 생성합니다.
     *
     * TODO: FundingRepository에 findAllByIdeaId(Long ideaId) 추가 요청 필요
     *       현재 페이징 방식만 존재하여 전체 조회 불가
     */
    @Transactional
    public void createCancelRefunds(Long ideaId) {
        createRefundsForIdea(ideaId, RefundReason.CANCELLED);
    }

    /**
     * 분쟁 환불 레코드 생성 (관리자 수동 처리)
     * reason을 직접 지정해야 하므로 별도 메서드로 분리
     * 단건 처리 (특정 후원자 한 명에 대한 환불)
     */
    @Transactional
    public RefundResponse createDisputeRefund(Long paymentId, Long sponsorId, Long amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        Refund refund = Refund.builder()
                .paymentId(payment.getId())
                .sponsorId(sponsorId)
                .amount(amount)
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
     *
     * TODO: FundingRepository.findAllByIdeaId() 추가 후 아래 주석 제거 필요
     *       현재는 임시로 페이징 없이 처리하나, 데이터 누락 위험 있음
     */
    private void createRefundsForIdea(Long ideaId, RefundReason reason) {
        // TODO: fundingRepository.findAllByIdeaId(ideaId) 로 변경 필요
        List<Funding> fundings = fundingRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        for (Funding funding : fundings) {
            List<Payment> payments = paymentRepository.findByFundingIdOrderByCreatedAtDesc(funding.getId());

            // SUCCESS 상태의 결제 건만 환불 처리
            payments.stream()
                    .filter(p -> p.getStatus() == PaymentTypes.PaymentStatus.SUCCESS)
                    .forEach(payment -> {
                        Refund refund = Refund.builder()
                                .paymentId(payment.getId())
                                .sponsorId(funding.getSponsorId())
                                .amount(payment.getAmount())
                                .reason(reason)
                                .build();
                        refundRepository.save(refund);
                    });
        }
    }
}