package com.team04.domain.settlement.service;

import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.service.IdeaService;
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
     * 누적 선정산 금액(FAILED 제외) 차감 후 실제 지급액 산출
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
     * SettlementScheduler에서 호출 — reason 고정 (GOAL_NOT_MET)
     * 수수료 없이 환불 처리, 선정산 차감 후 실제 환불액 산출
     * 멱등성 키로 중복 환불 방지
     */
    @Transactional
    public SettlementResponse createGoalNotMetRefundSettlement(Long ideaId) {
        return createRefundSettlementInternal(ideaId, "GOAL-NOT-MET");
    }

    /**
     * 이행 중단 환불 장부 생성
     * MilestoneService.cancelMilestone()에서 호출 — reason 고정 (CANCELLED)
     * 수수료 없이 환불 처리, 선정산 차감 후 실제 환불액 산출
     * 멱등성 키로 중복 환불 방지
     */
    @Transactional
    public SettlementResponse createCancelRefundSettlement(Long ideaId) {
        return createRefundSettlementInternal(ideaId, "CANCELLED");
    }

    /**
     * 환불 장부 생성 내부 공통 로직
     * reason별 멱등성 키를 분리하여 목표미달/이행중단 각각 독립적으로 관리
     */
    private SettlementResponse createRefundSettlementInternal(Long ideaId, String reasonSuffix) {
        String idempotencyKey = "idea-" + ideaId + "-REFUND-" + reasonSuffix;
        Long totalAmount = ideaService.getIdea(ideaId).currentAmount();

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        long preSettlementTotal = preSettlementRepository
                .sumAmountByIdeaIdAndStatus(ideaId, PreSettlementStatus.COMPLETED);

        long refundAmount = totalAmount - preSettlementTotal;

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(totalAmount)
                .platformFee(0L)
                .payoutAmount(refundAmount)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.refund();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }
}