package com.team04.domain.settlement.service;

import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.payment.entity.VbankLedgerType;
import com.team04.domain.payment.event.PreSettlementPayoutRequestedEvent;
import com.team04.domain.payment.service.VbankLedgerService;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreSettlementService {

    private final PreSettlementRepository preSettlementRepository;
    private final MilestoneRepository milestoneRepository;
    private final IdeaRepository ideaRepository;
    private final IdeaService ideaService;
    private final ApplicationEventPublisher eventPublisher;
    private final VbankLedgerService vbankLedgerService;

    /**
     * 선정산 신청
     * 마일스톤 IN_PROGRESS 상태에서만 가능
     * 요청한 제안자가 해당 아이디어 소유자인지 검증
     * Idea 비관락으로 같은 아이디어의 동시 선정산 요청 제어
     * 보증금 2배 한도 내에서 분할 신청 가능 (ideaId 기준 SUM 누적 체크)
     * spring-retry @Retryable로 최대 3회 재시도
     * payout()은 트랜잭션 커밋 이후 호출 — 롤백 시 실제 송금 방지
     */
    @Retryable(
            retryFor = PessimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    @Transactional
    public PreSettlementResponse requestPreSettlement(Long ideaId, PreSettlementRequest request, Long userId) {
        ideaService.validateNotSuspended(ideaId); // 분쟁 처리 중 일시 중단된 프로젝트는 선정산 신청 불가
        // 선정산 한도는 ideaId 기준 누적액으로 검증하므로, 동시 요청도 같은 ideaId 단위로 직렬화한다.
        ideaRepository.findByIdForUpdate(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        milestoneRepository.findByIdeaIdAndStatusWithPessimisticLock(ideaId, MilestoneStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.PRE_SETTLEMENT_MILESTONE_NOT_IN_PROGRESS));

        IdeaResponse idea = ideaService.getIdea(ideaId);
        if (!idea.userId().equals(userId)) {
            throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
        }

        long limit = idea.depositAmount() * 2;
        long accumulated = preSettlementRepository.sumAmountByIdeaIdAndStatusNot(
                ideaId, PreSettlementStatus.FAILED);

        if (accumulated + request.amount() > limit) {
            throw new CustomException(ErrorCode.PRE_SETTLEMENT_LIMIT_EXCEEDED);
        }

        PreSettlement preSettlement = PreSettlement.builder()
                .ideaId(ideaId)
                .amount(request.amount())
                .build();

        PreSettlement saved = preSettlementRepository.save(preSettlement);

        // 트랜잭션 커밋 이후 지급대행 요청 — 롤백 시 실제 송금 방지
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new PreSettlementPayoutRequestedEvent(saved.getId()));
            }
        });

        return PreSettlementResponse.from(saved);
    }

    /**
     * 선정산 신청 fallback — 3회 재시도 후 모두 실패 시 호출
     */
    @Recover
    public PreSettlementResponse requestPreSettlementRecover(PessimisticLockingFailureException e, Long ideaId, PreSettlementRequest request, Long userId) {
        log.error("선정산 신청 최종 실패 ideaId={}, amount={}", ideaId, request.amount(), e);
        throw new CustomException(ErrorCode.PRE_SETTLEMENT_REQUEST_FAILED);
    }

    /**
     * 선정산 지급 완료 처리
     * 결제팀이 실제 지급 완료 후 콜백으로 호출 (ADMIN 인증)
     */
    @Transactional
    public PreSettlementResponse completePreSettlement(Long preSettlementId) {
        PreSettlement preSettlement = preSettlementRepository.findById(preSettlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRE_SETTLEMENT_NOT_FOUND));
        preSettlement.complete();
        // 선정산 지급 성공이 확정된 뒤 실제 출금 내역을 아이디어 가상계좌 장부에 남긴다.
        vbankLedgerService.recordOut(
                preSettlement.getIdeaId(),
                VbankLedgerType.PRE_SETTLEMENT_PAID,
                preSettlement.getAmount(),
                "pre-settlement-" + preSettlement.getId() + "-PAID",
                "PreSettlement",
                preSettlement.getId(),
                "선정산 지급"
        );
        return PreSettlementResponse.from(preSettlement);
    }

    /**
     * 선정산 지급 실패 처리
     * 결제팀이 지급 실패 시 콜백으로 호출 (ADMIN 인증)
     * REQUESTED 상태를 FAILED로 전환하여 한도 차감 해제
     */
    @Transactional
    public PreSettlementResponse failPreSettlement(Long preSettlementId) {
        PreSettlement preSettlement = preSettlementRepository.findById(preSettlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRE_SETTLEMENT_NOT_FOUND));
        preSettlement.fail();
        return PreSettlementResponse.from(preSettlement);
    }

    /**
     * 지급 실패 건을 재처리 대기 상태로 되돌립니다.
     * 스케줄러가 다시 payout을 호출하기 전에 사용합니다.
     */
    @Transactional
    public PreSettlementResponse retryPreSettlementPayout(Long preSettlementId) {
        PreSettlement preSettlement = preSettlementRepository.findById(preSettlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRE_SETTLEMENT_NOT_FOUND));
        preSettlement.retry();
        return PreSettlementResponse.from(preSettlement);
    }

    /**
     * 아이디어별 선정산 내역 조회
     * 관리자는 모두 조회 가능, 제안자는 본인 프로젝트만 조회 가능
     */
    @Transactional(readOnly = true)
    public List<PreSettlementResponse> getPreSettlements(Long ideaId, Long userId, Role role) {
        if (role != Role.ADMIN) {
            IdeaResponse idea = ideaService.getIdea(ideaId);
            if (!idea.userId().equals(userId)) {
                throw new CustomException(ErrorCode.SETTLEMENT_ACCESS_DENIED);
            }
        }
        return preSettlementRepository.findByIdeaId(ideaId).stream()
                .map(PreSettlementResponse::from)
                .toList();
    }
}
