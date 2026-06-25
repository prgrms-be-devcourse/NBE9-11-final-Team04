package com.team04.domain.payment.service;

import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.payment.dto.response.VbankLedgerResponse;
import com.team04.domain.payment.entity.VbankLedger;
import com.team04.domain.payment.entity.VbankLedgerDirection;
import com.team04.domain.payment.entity.VbankLedgerType;
import com.team04.domain.payment.repository.VbankLedgerRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VbankLedgerService {

    private final VbankLedgerRepository vbankLedgerRepository;
    private final IdeaService ideaService;
    private final IdeaRepository ideaRepository;
    private final FundingRepository fundingRepository;

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
        return vbankLedgerRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId).stream()
                .map(VbankLedgerResponse::from)
                .toList();
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
        VbankLedgerResponse existing = vbankLedgerRepository.findByIdempotencyKey(idempotencyKey)
                .map(VbankLedgerResponse::from)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        // 같은 ideaId의 장부 기록이 동시에 들어오면 이전 잔액을 중복 조회할 수 있어 아이디어 row를 먼저 잠근다.
        ideaRepository.findByIdForUpdate(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        existing = vbankLedgerRepository.findByIdempotencyKey(idempotencyKey)
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
                    : Math.max(currentBalance - amount, 0);
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

    private long getCurrentBalance(Long ideaId) {
        return vbankLedgerRepository.findTopByIdeaIdAndAffectsBalanceOrderByIdDesc(ideaId, true)
                .map(VbankLedger::getBalanceAfter)
                .orElse(0L);
    }

    private void validateReadable(Long ideaId, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return;
        }

        IdeaResponse idea = ideaService.getIdea(ideaId);
        if (idea.userId().equals(userId)) {
            return;
        }

        if (fundingRepository.existsPaidSponsorByIdeaIdAndSponsorId(ideaId, userId)) {
            return;
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }
}
