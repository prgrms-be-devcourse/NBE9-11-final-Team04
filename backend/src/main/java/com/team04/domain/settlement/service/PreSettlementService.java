package com.team04.domain.settlement.service;

import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.settlement.dto.request.PreSettlementRequest;
import com.team04.domain.settlement.dto.response.PreSettlementResponse;
import com.team04.domain.settlement.entity.PreSettlement;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreSettlementService {

    private final PreSettlementRepository preSettlementRepository;
    private final MilestoneRepository milestoneRepository;
    private final IdeaService ideaService;

    /**
     * 선정산 신청
     * 마일스톤 IN_PROGRESS 상태에서만 가능
     * 요청한 제안자가 해당 아이디어 소유자인지 검증
     * Milestone 비관락으로 동시 요청 제어
     * 보증금 2배 한도 내에서 분할 신청 가능 (ideaId 기준 SUM 누적 체크)
     * spring-retry @Retryable로 최대 3회 재시도
     * 장부 생성 후 REQUESTED 상태 유지 — 결제팀이 지급 완료 후 COMPLETED로 변경
     * TODO: 한도 계산은 idea.getDepositAmount() * 2로 변경 필요 (idea 도메인에 depositAmount 필드 추가 요청)
     */
    @Retryable(
            retryFor = PessimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    @Transactional
    public PreSettlementResponse requestPreSettlement(Long milestoneId, PreSettlementRequest request, Long userId) {
        Milestone milestone = milestoneRepository.findByIdWithPessimisticLock(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));

        if (milestone.getStatus() != MilestoneStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.PRE_SETTLEMENT_MILESTONE_NOT_IN_PROGRESS);
        }

        IdeaResponse idea = ideaService.getIdea(milestone.getIdeaId());
        if (!idea.userId().equals(userId)) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }

        // TODO: idea.getDepositAmount() * 2로 변경 필요 (idea 도메인 담당자에게 depositAmount 필드 추가 요청)
        long limit = Math.round(idea.goalAmount() * 0.3) * 2;

        // Milestone 비관락으로 동시성 보장 — FAILED 제외 SUM으로 유효 누적액 계산
        long accumulated = preSettlementRepository.sumAmountByIdeaIdAndStatusNot(
                milestone.getIdeaId(), PreSettlementStatus.FAILED);

        if (accumulated + request.amount() > limit) {
            throw new CustomException(ErrorCode.PRE_SETTLEMENT_LIMIT_EXCEEDED);
        }

        PreSettlement preSettlement = PreSettlement.builder()
                .milestoneId(milestoneId)
                .ideaId(milestone.getIdeaId())
                .amount(request.amount())
                .build();

        // TODO: 결제팀에 지급 요청 (PaymentService.payout()) 호출 후 REQUESTED 유지
        // 결제팀이 지급 완료 후 PATCH /pre-settlements/{preSettlementId}/complete 호출
        return PreSettlementResponse.from(preSettlementRepository.save(preSettlement));
    }

    /**
     * 선정산 신청 fallback — 3회 재시도 후 모두 실패 시 호출
     * CustomException(비즈니스 예외)은 그대로 throw — 삼키지 않음
     */
    @Recover
    public PreSettlementResponse requestPreSettlementRecover(PessimisticLockingFailureException e, Long milestoneId, PreSettlementRequest request, Long userId) {
        log.error("선정산 신청 최종 실패 milestoneId={}, amount={}", milestoneId, request.amount(), e);
        throw new CustomException(ErrorCode.PRE_SETTLEMENT_REQUEST_FAILED);
    }

    /**
     * 선정산 지급 완료 처리
     * 결제팀이 실제 지급 완료 후 호출
     * TODO: 결제팀과 호출 방식 협의 후 인증 처리 변경 필요 (현재 ADMIN으로 임시 처리)
     */
    @Transactional
    public PreSettlementResponse completePreSettlement(Long preSettlementId) {
        PreSettlement preSettlement = preSettlementRepository.findById(preSettlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRE_SETTLEMENT_NOT_FOUND));

        preSettlement.complete();
        return PreSettlementResponse.from(preSettlement);
    }

    /**
     * 마일스톤별 선정산 내역 조회
     * 관리자는 모두 조회 가능, 제안자는 본인 프로젝트만 조회 가능
     */
    @Transactional(readOnly = true)
    public List<PreSettlementResponse> getPreSettlements(Long milestoneId, Long userId, Role role) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));

        if (role != Role.ADMIN) {
            IdeaResponse idea = ideaService.getIdea(milestone.getIdeaId());
            if (!idea.userId().equals(userId)) {
                throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
            }
        }

        return preSettlementRepository.findByMilestoneId(milestoneId).stream()
                .map(PreSettlementResponse::from)
                .toList();
    }
}