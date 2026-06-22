package com.team04.domain.funding.service;

import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.funding.entity.FundingTypes.DepositStatus;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.dto.request.PayDepositRequest;
import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositRepository depositRepository;
    private final IdeaRepository ideaRepository;

    @Transactional(readOnly = true)
    public DepositResponse getDeposit(Long ideaId) {
        Deposit deposit = depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESCROW_NOT_FOUND));
        return DepositResponse.from(deposit);
    }

    /** 창작자 보증금 납부 — PG 연동 전 Mock 납부 완료(HELD) 처리 */
    @Transactional
    public DepositResponse payDeposit(Long ideaId, Long userId, PayDepositRequest request) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.validateOwner(userId);

        if (depositRepository.existsByIdeaIdAndStatus(ideaId, DepositStatus.HELD)) {
            throw new CustomException(ErrorCode.PROJECT_FEE_ALREADY_PAID);
        }

        if (request.amount() == null || request.amount() < 1) {
            throw new CustomException(ErrorCode.INVALID_FUNDING_AMOUNT);
        }

        Deposit deposit = depositRepository.save(
                Deposit.createHeld(ideaId, userId, request.amount())
        );
        return DepositResponse.from(deposit);
    }

    @Transactional
    public DepositResponse releaseDeposit(Long ideaId, Long userId) {
        Deposit deposit = getHeldDeposit(ideaId, userId);
        deposit.release();
        return DepositResponse.from(deposit);
    }

    @Transactional
    public DepositResponse forfeitDeposit(Long ideaId) {
        Deposit deposit = depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESCROW_NOT_FOUND));
        deposit.forfeit();
        return DepositResponse.from(deposit);
    }

    private Deposit getHeldDeposit(Long ideaId, Long userId) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.validateOwner(userId);

        Deposit deposit = depositRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.ESCROW_NOT_FOUND));
        return deposit;
    }
}
