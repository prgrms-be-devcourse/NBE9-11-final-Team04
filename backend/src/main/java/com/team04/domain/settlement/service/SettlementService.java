package com.team04.domain.settlement.service;

import com.team04.domain.funding.service.FundingService;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.entity.VbankLedgerType;
import com.team04.domain.payment.event.SettlementPayoutRequestedEvent;
import com.team04.domain.payment.service.VbankLedgerService;
import com.team04.domain.settlement.dto.response.SettlementResponse;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.entity.Settlement;
import com.team04.domain.settlement.entity.SettlementStatus;
import com.team04.domain.settlement.entity.SettlementType;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.settlement.repository.SettlementRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final double PLATFORM_FEE_RATE = 0.01;

    private final SettlementRepository settlementRepository;
    private final IdeaService ideaService;
    private final IdeaRepository ideaRepository;
    private final PreSettlementRepository preSettlementRepository;
    private final RefundService refundService;
    private final FundingService fundingService;
    private final ApplicationEventPublisher eventPublisher;
    private final VbankLedgerService vbankLedgerService;

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

        return saveAndRequestPayout(settlement, SettlementStatus.COMPLETED);
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

        return saveAndRequestPayout(settlement, SettlementStatus.DEPOSIT_REFUNDED);
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

        return saveAndRequestPayout(settlement, SettlementStatus.DEPOSIT_REFUNDED);
    }

    /**
     * 관리자 보증금 환급 판정 장부 생성.
     * 직접 Deposit 상태를 바꾸지 않고 지급 성공 콜백에서 REFUNDED로 전환하여 장부와 실제 지급 흐름을 맞춘다.
     */
    @Transactional
    public SettlementResponse createAdminDepositRefundSettlement(Long ideaId) {
        // 관리자 보증금 환급/몰수 판정은 같은 ideaId에서 하나만 가능해야 하므로 아이디어 단위로 직렬화한다.
        lockIdeaForDepositDecision(ideaId);
        if (settlementExists(ideaId, "DEPOSIT-FORFEITED")
                || settlementExists(ideaId, "DEPOSIT-COMPLETED")
                || settlementExists(ideaId, "DEPOSIT-GOAL-NOT-MET")
                || settlementExists(ideaId, "DEPOSIT-REFUND")) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        String idempotencyKey = "idea-" + ideaId + "-DEPOSIT-ADMIN-RELEASE";

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
                .memo("관리자 보증금 환급 판정")
                .build();

        return saveAndRequestPayout(settlement, SettlementStatus.DEPOSIT_REFUNDED);
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
            return saveAndRequestPayout(settlement, SettlementStatus.PARTIALLY_REFUNDED);
        } else {
            settlement.depositExhausted();
        }
        return SettlementResponse.from(settlementRepository.save(settlement));
    }
    /**
     * 정당한 사유 중단 후원자 환불 장부 생성
     * MilestoneService.refundMilestone()에서 호출
     * 후원금 잔액만 환불 재원 (보증금은 createDepositRefundSettlement에서 별도 처리)
     */
    @Transactional
    public SettlementResponse createJustifiedCancelRefundSettlement(Long ideaId) {
        String idempotencyKey = "idea-" + ideaId + "-REFUND-JUSTIFIED";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        IdeaResponse idea = ideaService.getIdea(ideaId);
        long fundingAmount = idea.currentAmount();
        long preSettlementTotal = preSettlementRepository
                .sumAmountByIdeaIdAndStatus(ideaId, PreSettlementStatus.COMPLETED);

        long fundingBalance = Math.max(fundingAmount - preSettlementTotal, 0);

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(fundingAmount)
                .platformFee(0L)
                .payoutAmount(fundingBalance)
                .idempotencyKey(idempotencyKey)
                .build();

        settlement.refund();
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    /**
     * 단순 포기/먹튀 후원자 환불 장부 생성
     * MilestoneService.cancelMilestone() / forfeitMilestone()에서 호출
     * 후원금 잔액만 환불 재원 (보증금은 createDepositForfeitSettlement에서 별도 처리)
     */
    @Transactional
    public SettlementResponse createCancelRefundSettlement(Long ideaId) {
        return createCancelRefundSettlement(ideaId, null);
    }

    @Transactional
    public SettlementResponse createCancelRefundSettlement(Long ideaId, String memo) {
        String idempotencyKey = "idea-" + ideaId + "-REFUND-CANCELLED";

        if (settlementRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        IdeaResponse idea = ideaService.getIdea(ideaId);
        long fundingAmount = idea.currentAmount();
        long preSettlementTotal = preSettlementRepository
                .sumAmountByIdeaIdAndStatus(ideaId, PreSettlementStatus.COMPLETED);

        long fundingBalance = Math.max(fundingAmount - preSettlementTotal, 0);

        Settlement settlement = Settlement.builder()
                .ideaId(ideaId)
                .type(SettlementType.FINAL)
                .totalAmount(fundingAmount)
                .platformFee(0L)
                .payoutAmount(fundingBalance)
                .idempotencyKey(idempotencyKey)
                .memo(memo)
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
        return createDepositForfeitSettlement(ideaId, null);
    }

    @Transactional
    public SettlementResponse createDepositForfeitSettlement(Long ideaId, String memo) {
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
                .memo(memo)
                .build();

        settlement.forfeit();
        Settlement saved = settlementRepository.save(settlement);
        // 보증금 몰수는 외부 지급이 없으므로 잔액 차감 없이 후원자 공개용 장부로만 남긴다.
        vbankLedgerService.recordDisclosureOut(
                ideaId,
                VbankLedgerType.DEPOSIT_FORFEITED,
                depositAmount,
                "settlement-" + saved.getId() + "-DEPOSIT-FORFEITED",
                "Settlement",
                saved.getId(),
                "보증금 몰수 판정"
        );
        return SettlementResponse.from(saved);
    }

    /**
     * 관리자 보증금 몰수 판정.
     * 몰수는 외부 지급이 없으므로 정산/가상계좌 공개 장부를 남긴 뒤 Deposit 상태를 즉시 FORFEITED로 전환한다.
     */
    @Transactional
    public SettlementResponse forfeitDepositByAdmin(Long ideaId) {
        // 관리자 보증금 환급/몰수 판정은 같은 ideaId에서 하나만 가능해야 하므로 아이디어 단위로 직렬화한다.
        lockIdeaForDepositDecision(ideaId);
        if (settlementExists(ideaId, "DEPOSIT-ADMIN-RELEASE")
                || settlementExists(ideaId, "DEPOSIT-COMPLETED")
                || settlementExists(ideaId, "DEPOSIT-GOAL-NOT-MET")
                || settlementExists(ideaId, "DEPOSIT-REFUND")) {
            throw new CustomException(ErrorCode.SETTLEMENT_DUPLICATE);
        }

        SettlementResponse response = createDepositForfeitSettlement(ideaId, "관리자 보증금 몰수 판정");
        fundingService.forfeitDeposit(ideaId);
        return response;
    }

    /**
     * 에스크로 강제 환불 (관리자 전용)
     * 단순 포기/먹튀 케이스에서 관리자가 판정 시 호출
     * 후원금 잔액 환불 장부 + 보증금 몰수 장부 + 후원자 환불 레코드 + 보증금 몰수
     * 하나의 트랜잭션으로 묶어 정합성 보장
     */
    @Transactional
    public void forceRefund(Long ideaId) {
        forceRefund(ideaId, null);
    }

    @Transactional
    public void forceRefund(Long ideaId, String reason) {
        if (!settlementExists(ideaId, "REFUND-CANCELLED")) {
            createCancelRefundSettlement(ideaId, reason);
        } else {
            recordSettlementMemo(ideaId, "REFUND-CANCELLED", reason);
        }
        if (!settlementExists(ideaId, "DEPOSIT-FORFEITED")) {
            createDepositForfeitSettlement(ideaId, reason);
        } else {
            recordSettlementMemo(ideaId, "DEPOSIT-FORFEITED", reason);
        }
        refundService.createCancelRefunds(ideaId, false);
    }

    private boolean settlementExists(Long ideaId, String suffix) {
        return settlementRepository.findByIdempotencyKey("idea-" + ideaId + "-" + suffix).isPresent();
    }

    private void lockIdeaForDepositDecision(Long ideaId) {
        ideaRepository.findByIdForUpdate(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
    }

    @Transactional
    public SettlementResponse completeSettlementPayout(Long settlementId, SettlementStatus successStatus) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlement.completeAs(successStatus);
        recordSettlementPayoutLedger(settlement, successStatus);
        // 보증금 환급은 지급 성공이 확인된 뒤에만 Deposit 상태를 REFUNDED로 전환한다.
        releaseDepositIfNeeded(settlement, successStatus);
        return SettlementResponse.from(settlement);
    }

    @Transactional
    public SettlementResponse failSettlementPayout(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlement.fail();
        return SettlementResponse.from(settlement);
    }

    /**
     * 지급 실패 건을 재처리 대기 상태로 되돌립니다.
     * 스케줄러가 다시 payout을 호출하기 전에 사용합니다.
     */
    @Transactional
    public SettlementResponse retrySettlementPayout(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlement.retryPayout();
        return SettlementResponse.from(settlement);
    }

    private SettlementResponse saveAndRequestPayout(Settlement settlement, SettlementStatus successStatus) {
        Settlement saved = settlementRepository.save(settlement);
        if (saved.getPayoutAmount() <= 0) {
            // 지급액이 없으면 PG/mock 호출 없이 장부 상태만 완료 처리한다.
            saved.completeAs(successStatus);
            releaseDepositIfNeeded(saved, successStatus);
            return SettlementResponse.from(saved);
        }

        // 정산 장부 커밋 이후 지급대행 요청 — 롤백 시 실제 지급 요청이 나가지 않도록 한다.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new SettlementPayoutRequestedEvent(saved.getId(), successStatus));
            }
        });

        return SettlementResponse.from(saved);
    }

    private void releaseDepositIfNeeded(Settlement settlement, SettlementStatus successStatus) {
        if (successStatus == SettlementStatus.DEPOSIT_REFUNDED
                || successStatus == SettlementStatus.PARTIALLY_REFUNDED) {
            fundingService.releaseDeposit(settlement.getIdeaId());
        }
    }

    private void recordSettlementPayoutLedger(Settlement settlement, SettlementStatus successStatus) {
        if (settlement.getPayoutAmount() <= 0) {
            return;
        }

        VbankLedgerType type = switch (successStatus) {
            case COMPLETED -> VbankLedgerType.FINAL_SETTLEMENT_PAID;
            case DEPOSIT_REFUNDED, PARTIALLY_REFUNDED -> VbankLedgerType.DEPOSIT_REFUNDED;
            default -> null;
        };
        if (type == null) {
            return;
        }

        // 최종 정산금 또는 보증금 환급 지급 성공 후 실제 출금 내역을 가상계좌 장부에 반영한다.
        vbankLedgerService.recordOut(
                settlement.getIdeaId(),
                type,
                settlement.getPayoutAmount(),
                "settlement-" + settlement.getId() + "-" + successStatus,
                "Settlement",
                settlement.getId(),
                successStatus == SettlementStatus.COMPLETED ? "최종 정산 지급" : "보증금 환급 지급"
        );
    }

    private void recordSettlementMemo(Long ideaId, String suffix, String memo) {
        // 관리자 강제 환불 사유를 이미 존재하는 정산 장부에도 남긴다.
        settlementRepository.findByIdempotencyKey("idea-" + ideaId + "-" + suffix)
                .ifPresent(settlement -> settlement.recordMemo(memo));
    }
}
