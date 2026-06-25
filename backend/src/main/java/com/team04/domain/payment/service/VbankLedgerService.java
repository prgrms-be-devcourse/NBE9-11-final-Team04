package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.funding.entity.FundingTypes.DepositStatus;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.dto.response.VbankLedgerResponse;
import com.team04.domain.payment.entity.VbankLedger;
import com.team04.domain.payment.entity.VbankLedgerDirection;
import com.team04.domain.payment.entity.VbankLedgerType;
import com.team04.domain.payment.repository.VbankLedgerRepository;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VbankLedgerService {

    private final VbankLedgerRepository vbankLedgerRepository;
    private final IdeaService ideaService;
    private final IdeaRepository ideaRepository;
    private final FundingRepository fundingRepository;
    private final DepositRepository depositRepository;
    private final PreSettlementRepository preSettlementRepository;

    @Transactional
    public VbankLedgerResponse recordIn(
            Long ideaId,
            VbankLedgerType type,
            Long amount,
            String idempotencyKey,
            String referenceType,
            Long referenceId,
            String memo
    ) {
        return record(ideaId, type, VbankLedgerDirection.IN, amount, true, idempotencyKey,
                referenceType, referenceId, memo);
    }

    @Transactional
    public VbankLedgerResponse recordOut(
            Long ideaId,
            VbankLedgerType type,
            Long amount,
            String idempotencyKey,
            String referenceType,
            Long referenceId,
            String memo
    ) {
        return record(ideaId, type, VbankLedgerDirection.OUT, amount, true, idempotencyKey,
                referenceType, referenceId, memo);
    }

    @Transactional
    public VbankLedgerResponse recordDisclosureOut(
            Long ideaId,
            VbankLedgerType type,
            Long amount,
            String idempotencyKey,
            String referenceType,
            Long referenceId,
            String memo
    ) {
        // 선정산 지급처럼 이미 잔액을 줄인 뒤의 사용 내역은 후원자 공개용으로만 남기고 잔액은 다시 차감하지 않는다.
        return record(ideaId, type, VbankLedgerDirection.OUT, amount, false, idempotencyKey,
                referenceType, referenceId, memo);
    }

    @Transactional(readOnly = true)
    public List<VbankLedgerResponse> getLedgers(Long ideaId, Long userId, Role role) {
        validateReadable(ideaId, userId, role);
        return vbankLedgerRepository.findByIdeaIdOrderByIdDesc(ideaId).stream()
                .map(VbankLedgerResponse::from)
                .toList();
    }

    /**
     * 외부 PG/payout 호출 전에 실제 출금 장부 저장 가능 여부를 먼저 확인한다.
     * 기존 프로젝트처럼 장부가 비어 있으면 현재 상태 기준 시작 잔액을 1회 생성한다.
     */
    @Transactional
    public void validateSufficientBalanceForOut(Long ideaId, Long amount) {
        validateRecordRequest(
                ideaId,
                VbankLedgerType.LEGACY_OPENING_BALANCE,
                VbankLedgerDirection.OUT,
                amount,
                "precheck-" + ideaId
        );
        ideaRepository.findByIdForUpdate(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        ensureLegacyOpeningBalanceIfNeeded(ideaId);

        long currentBalance = getCurrentBalance(ideaId);
        if (currentBalance - amount < 0) {
            throw new CustomException(ErrorCode.VBANK_LEDGER_INSUFFICIENT_BALANCE);
        }
    }

    private VbankLedgerResponse record(
            Long ideaId,
            VbankLedgerType type,
            VbankLedgerDirection direction,
            Long amount,
            boolean affectsBalance,
            String idempotencyKey,
            String referenceType,
            Long referenceId,
            String memo
    ) {
        validateRecordRequest(ideaId, type, direction, amount, idempotencyKey);

        VbankLedgerResponse existing = vbankLedgerRepository.findByIdempotencyKey(idempotencyKey)
                .map(VbankLedgerResponse::from)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        // 같은 ideaId의 장부 기록이 동시에 들어오면 이전 잔액을 중복 조회할 수 있어 아이디어 row를 먼저 잠근다.
        ideaRepository.findByIdForUpdate(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        if (direction == VbankLedgerDirection.OUT && affectsBalance) {
            ensureLegacyOpeningBalanceIfNeeded(ideaId);
        }
        // 락 대기 중 다른 트랜잭션이 같은 멱등키 장부를 저장했을 수 있어, 락 조회로 한 번 더 확인한다.
        existing = vbankLedgerRepository.findByIdempotencyKeyForUpdate(idempotencyKey)
                .map(VbankLedgerResponse::from)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        long currentBalance = getCurrentBalance(ideaId);
        long balanceAfter = currentBalance;
        if (affectsBalance) {
            // 실제 입출금 성격의 기록만 프로젝트 가상계좌 잔액에 반영한다.
            balanceAfter = direction == VbankLedgerDirection.IN
                    ? currentBalance + amount
                    : currentBalance - amount;
            if (balanceAfter < 0) {
                // 부족 출금을 0으로 보정하면 장부 수식이 깨지므로 저장하지 않고 실패시킨다.
                throw new CustomException(ErrorCode.VBANK_LEDGER_INSUFFICIENT_BALANCE);
            }
        }

        VbankLedger ledger = VbankLedger.create(
                ideaId,
                type,
                direction,
                amount,
                balanceAfter,
                affectsBalance,
                idempotencyKey,
                referenceType,
                referenceId,
                memo
        );
        return VbankLedgerResponse.from(vbankLedgerRepository.save(ledger));
    }

    private void ensureLegacyOpeningBalanceIfNeeded(Long ideaId) {
        if (findLatestBalanceLedgerForUpdate(ideaId) != null) {
            return;
        }

        long openingBalance = calculateLegacyOpeningBalance(ideaId);
        if (openingBalance <= 0) {
            return;
        }

        VbankLedger openingLedger = VbankLedger.create(
                ideaId,
                VbankLedgerType.LEGACY_OPENING_BALANCE,
                VbankLedgerDirection.IN,
                openingBalance,
                openingBalance,
                true,
                "legacy-opening-" + ideaId,
                "Idea",
                ideaId,
                "기존 데이터 기준 가상계좌 시작 잔액"
        );
        vbankLedgerRepository.save(openingLedger);
    }

    private long calculateLegacyOpeningBalance(Long ideaId) {
        IdeaResponse idea = ideaService.getIdea(ideaId);
        long currentFunding = idea.currentAmount() != null ? idea.currentAmount() : 0L;
        long heldDeposit = depositRepository.findByIdeaId(ideaId)
                .filter(deposit -> deposit.getStatus() == DepositStatus.HELD)
                .map(Deposit::getAmount)
                .orElse(0L);
        Long completedPreSettlement = preSettlementRepository.sumAmountByIdeaIdAndStatus(
                ideaId,
                PreSettlementStatus.COMPLETED
        );
        long completedPreSettlementAmount = completedPreSettlement != null ? completedPreSettlement : 0L;
        return Math.max(currentFunding + heldDeposit - completedPreSettlementAmount, 0L);
    }

    private void validateRecordRequest(
            Long ideaId,
            VbankLedgerType type,
            VbankLedgerDirection direction,
            Long amount,
            String idempotencyKey
    ) {
        if (ideaId == null || type == null || direction == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("가상계좌 장부 필수 값이 누락되었습니다");
        }
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("장부 금액은 0 이상이어야 합니다");
        }
    }

    private long getCurrentBalance(Long ideaId) {
        VbankLedger latestLedger = findLatestBalanceLedgerForUpdate(ideaId);
        return latestLedger != null ? latestLedger.getBalanceAfter() : 0L;
    }

    private VbankLedger findLatestBalanceLedgerForUpdate(Long ideaId) {
        return vbankLedgerRepository.findLatestByIdeaIdAndAffectsBalanceForUpdate(
                        ideaId,
                        true,
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .orElse(null);
    }

    private void validateReadable(Long ideaId, Long userId, Role role) {
        if (userId == null || role == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (role == Role.ADMIN) {
            return;
        }

        IdeaResponse idea = ideaService.getIdea(ideaId);
        if (Objects.equals(idea.userId(), userId)) {
            return;
        }

        if (fundingRepository.existsPaidSponsorByIdeaIdAndSponsorId(ideaId, userId)) {
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }
}
