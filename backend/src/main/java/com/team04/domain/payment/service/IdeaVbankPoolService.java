package com.team04.domain.payment.service;

import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.IdeaVbankPool;
import com.team04.domain.payment.entity.IdeaVbankPoolStatus;
import com.team04.domain.payment.entity.VirtualAccount;
import com.team04.domain.payment.repository.IdeaVbankPoolRepository;
import com.team04.domain.payment.repository.VirtualAccountRepository;
import com.team04.global.config.payment.PaymentProperties;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 아이디어당 가상계좌 풀 관리.
 *
 * <p>Mock PG: 펀딩 오픈 시 1회 발급, 후원자들이 동일 계좌로 입금 (결제별 orderId는 유지).
 * Toss PG: 첫 가상계좌 confirm 시 풀 등록(참조용), 이후 후원은 결제당 VA 발급(토스 제약).
 */
@Service
@RequiredArgsConstructor
public class IdeaVbankPoolService {

    private static final long POOL_PLACEHOLDER_AMOUNT = 1L;

    private final IdeaVbankPoolRepository ideaVbankPoolRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentProperties paymentProperties;

    @Transactional(readOnly = true)
    public Optional<IdeaVbankPool> findActivePool(Long ideaId) {
        return ideaVbankPoolRepository.findByIdeaIdAndStatus(ideaId, IdeaVbankPoolStatus.ACTIVE);
    }

    /** Mock: 펀딩 오픈·첫 VBANK 후원 시 풀 가상계좌를 1회 발급합니다. */
    @Transactional
    public IdeaVbankPool ensurePoolForIdea(Long ideaId) {
        if (!paymentProperties.pool().enabled()) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        return ideaVbankPoolRepository.findByIdeaIdAndStatus(ideaId, IdeaVbankPoolStatus.ACTIVE)
                .orElseGet(() -> createMockPool(ideaId));
    }

    /** Toss: 첫 가상계좌 confirm 결과를 풀에 등록합니다. */
    @Transactional
    public void registerPoolFromConfirm(Long ideaId, String poolOrderId, VirtualAccountIssueResult issued) {
        if (!paymentProperties.pool().enabled()) {
            return;
        }
        if (ideaVbankPoolRepository.findByIdeaIdAndStatus(ideaId, IdeaVbankPoolStatus.ACTIVE).isPresent()) {
            return;
        }

        VirtualAccount saved = virtualAccountRepository.save(VirtualAccount.create(
                poolOrderId,
                issued.bankCode(),
                issued.accountNumber(),
                issued.dueDate(),
                POOL_PLACEHOLDER_AMOUNT
        ));

        ideaVbankPoolRepository.save(IdeaVbankPool.createActive(ideaId, poolOrderId, saved.getId()));
    }

    @Transactional(readOnly = true)
    public VirtualAccountIssueResult toIssueResult(IdeaVbankPool pool) {
        VirtualAccount account = virtualAccountRepository.findById(pool.getVirtualAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        return new VirtualAccountIssueResult(
                account.getId(),
                account.getBankCode(),
                account.getAccountNumber(),
                account.getDueDate()
        );
    }

    @Transactional(readOnly = true)
    public boolean usePoolForCreateTimeVbank() {
        return paymentProperties.pool().enabled() && paymentGateway.issuesVirtualAccountAtCreateTime();
    }

    private IdeaVbankPool createMockPool(Long ideaId) {
        String poolOrderId = "pool-" + ideaId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        VirtualAccountIssueResult issued = paymentGateway.issueVirtualAccount(poolOrderId, POOL_PLACEHOLDER_AMOUNT);
        VirtualAccount saved = virtualAccountRepository.save(VirtualAccount.create(
                poolOrderId,
                issued.bankCode(),
                issued.accountNumber(),
                issued.dueDate(),
                POOL_PLACEHOLDER_AMOUNT
        ));
        return ideaVbankPoolRepository.save(IdeaVbankPool.createActive(
                ideaId, poolOrderId, saved.getId()));
    }
}
