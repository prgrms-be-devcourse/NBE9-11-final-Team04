package com.team04.domain.settlement.service;

import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.service.MilestoneService;
import com.team04.domain.settlement.dto.response.SettlementResponse;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementType;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.SettlementRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final double PLATFORM_FEE_RATE = 0.01;

    private final SettlementRepository settlementRepository;
    private final IdeaService ideaService;
    private final PreSettlementRepository preSettlementRepository;

    /**
     * 프로젝트별 정산 이력 전체 조회
     * 관리자는 모든 프로젝트 조회 가능, 제안자는 본인 프로젝트만 조회 가능
     */
    @Transactional(readOnly = true)
    public List<SettlementResponse> getSettlementsByIdea(Long ideaId, Long userId, Role role) {
        if (role != Role.ADMIN) {
            IdeaResponse idea = ideaService.getIdea(ideaId);
            if (!idea.userId().equals(userId)) {
                throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
            }
        }
        return settlementRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId)
                .stream()
                .map(SettlementResponse::from)
                .toList();
    }

    /**
     * 정산 단건 조회
     * 관리자는 모든 정산 조회 가능, 제안자는 본인 프로젝트 정산만 조회 가능
     */
    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long settlementId, Long userId, Role role) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (role != Role.ADMIN) {
            IdeaResponse idea = ideaService.getIdea(settlement.getIdeaId());
            if (!idea.userId().equals(userId)) {
                throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
            }
        }
        return SettlementResponse.from(settlement);
    }

    /**
     * 최종 정산 장부 생성
     * 플랫폼 수수료 1% 차감 후 제안자 지급액 계산
     * 누적 선정산 금액(COMPLETED) 차감 후 실제 지급액 산출
     * 멱등성 키로 중복 정산 방지
     * 마일스톤 3단계 완료 승인 시 내부 호출
     */
    @Transactional
    public SettlementResponse createFinalSettlement(Long ideaId) {
        String idempotencyKey = "idea-" + ideaId + "-FINAL";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        Long totalAmount = ideaService.getIdea(ideaId).currentAmount();
        long preSettlementTotal = preSettlementRepository
                .sumAmountByIdeaIdAndStatus(ideaId, PreSettlementStatus.COMPLETED);

        long platformFee = Math.round(totalAmount * PLATFORM_FEE_RATE);
        long payoutAmount = totalAmount - platformFee - preSettlementTotal;

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .payoutAmount(payoutAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.complete();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    /**
     * 목표 미달성 환불 장부 생성
     * 후원금 전액 환불 — 선정산 없으므로 전액이 환불 재원
     * SettlementScheduler에서 호출
     */
    @Transactional
    public SettlementResponse createGoalNotMetRefundSettlement(Long ideaId) {
        String idempotencyKey = "idea-" + ideaId + "-REFUND-GOAL-NOT-MET";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        Long totalAmount = ideaService.getIdea(ideaId).currentAmount();

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(totalAmount)
                .platformFee(0L)
                .payoutAmount(totalAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.refund();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    /**
     * 목표 미달성 시 보증금 전액 제안자 환급 장부 생성
     * SettlementScheduler에서 호출
     */
    @Transactional
    public SettlementResponse createGoalNotMetDepositRefundSettlement(Long ideaId) {
        String idempotencyKey = "idea-" + ideaId + "-DEPOSIT-GOAL-NOT-MET";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        long depositAmount = ideaService.getIdea(ideaId).depositAmount();

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(depositAmount)
                .platformFee(0L)
                .payoutAmount(depositAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.depositRefund();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    /**
     * 최종 완성 시 보증금 전액 제안자 환급 장부 생성
     * MilestoneService.approveCompletionReport()에서 호출
     */
    @Transactional
    public SettlementResponse createCompletedDepositRefundSettlement(Long ideaId) {
        String idempotencyKey = "idea-" + ideaId + "-DEPOSIT-COMPLETED";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        long depositAmount = ideaService.getIdea(ideaId).depositAmount();

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(depositAmount)
                .platformFee(0L)
                .payoutAmount(depositAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.complete();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    /**
     * 제안자 보증금 환급 장부 생성 (정당한 사유 중단 시)
     * MilestoneService.refundMilestone()에서 호출
     *
     * 환급액 = max(보증금 - 선정산 완료액, 0)
     * 선정산이 보증금을 초과한 경우 환급액 0으로 기록 — "환급 시도했으나 잔액 없음" 명시
     */
    @Transactional
    public SettlementResponse createDepositRefundSettlement(Long ideaId) {
        String idempotencyKey = "idea-" + ideaId + "-DEPOSIT-REFUND";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        IdeaResponse idea = ideaService.getIdea(ideaId);
        long depositAmount = idea.depositAmount();
        long preSettlementTotal = preSettlementRepository
                .sumAmountByIdeaIdAndStatus(ideaId, PreSettlementStatus.COMPLETED);

        // 환급액 = max(보증금 - 선정산, 0)
        long refundAmount = Math.max(depositAmount - preSettlementTotal, 0);

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(depositAmount)
                .platformFee(0L)
                .payoutAmount(refundAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        if (refundAmount > 0) {
            settlement.partialRefund();
        } else {
            settlement.depositExhausted();
        }
        return SettlementResponse.from(settlementRepository.save(settlement));
    }
    /**
     * 이행 중단 후원자 환불 장부 생성
     * MilestoneService.refundMilestone() / cancelMilestone() / forfeitMilestone()에서 호출
     *
     * isJustified=true  (정당한 사유): 후원금 잔액만 환불 재원. 보증금 잔액은 제안자 환급.
     * isJustified=false (포기/먹튀):   후원금 잔액만 환불 재원. 보증금은 별도 몰수 장부.
     *
     * TODO: 정욱님 잔액 추적 PR 머지 후 totalAmount 계산 로직 교체
     */
    @Transactional
    public SettlementResponse createCancelRefundSettlement(Long ideaId, boolean isJustified) {
        String suffix = isJustified ? "JUSTIFIED" : "CANCELLED";
        String idempotencyKey = "idea-" + ideaId + "-REFUND-" + suffix;

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        IdeaResponse idea = ideaService.getIdea(ideaId);
        long fundingAmount = idea.currentAmount();
        long depositAmount = idea.depositAmount();
        long preSettlementTotal = preSettlementRepository
                .sumAmountByIdeaIdAndStatus(ideaId, PreSettlementStatus.COMPLETED);

        // 후원금 잔액 = 총 후원금 - 선정산액
        long fundingBalance = Math.max(fundingAmount - preSettlementTotal, 0);

        // 총 환불 재원 — 포기/먹튀는 보증금 별도 장부로 분리하므로 후원금 잔액만
        long totalRefundAmount = fundingBalance;

        // 장부상 totalAmount
        long totalAmount = fundingAmount;

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(totalAmount)
                .platformFee(0L)
                .payoutAmount(totalRefundAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.refund();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }
    /**
     * 보증금 몰수 장부 생성 (단순 포기 / 먹튀)
     * MilestoneService.cancelMilestone() / forfeitMilestone()에서 호출
     * 보증금 전액 몰수 후 후원자에게 분배
     */
    @Transactional
    public SettlementResponse createDepositForfeitSettlement(Long ideaId) {
        String idempotencyKey = "idea-" + ideaId + "-DEPOSIT-FORFEITED";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        long depositAmount = ideaService.getIdea(ideaId).depositAmount();

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(depositAmount)
                .platformFee(0L)
                .payoutAmount(depositAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.forfeit();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }
}