package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.event.FundingPaidEvent;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.dto.request.ConfirmPaymentRequest;
import com.team04.domain.payment.dto.request.CreatePaymentRequest;
import com.team04.domain.payment.dto.response.ConfirmPrepare;
import com.team04.domain.payment.dto.response.CreatedPayment;
import com.team04.domain.payment.dto.response.PaymentConfirmResult;
import com.team04.domain.payment.dto.response.PaymentResponse;
import com.team04.domain.payment.dto.response.PaymentSessionResult;
import com.team04.domain.payment.dto.response.PaymentVerifyResult;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.entity.PaymentWebhookLog;
import com.team04.domain.payment.entity.VirtualAccount;
import com.team04.domain.payment.entity.VbankDeposit;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.payment.repository.PaymentWebhookLogRepository;
import com.team04.domain.payment.repository.VirtualAccountRepository;
import com.team04.domain.payment.repository.VbankDepositRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import com.team04.domain.user.entity.Role;
import com.team04.global.config.payment.PaymentProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 도메인 비즈니스 로직을 담당하는 서비스입니다.
 * 결제 생성·승인·환불, 가상계좌 발급·웹훅, 조회를 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final VbankDepositRepository vbankDepositRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final PaymentWebhookLogRepository paymentWebhookLogRepository;
    private final FundingRepository fundingRepository;
    private final IdeaRepository ideaRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<PaymentService> selfProvider;
    private final PaymentProperties paymentProperties;

    // ── 결제 생성 ──────────────────────────────────────────────────────────

    // 후원(funding)에 대한 결제 세션 생성 — 후원자 본인만 호출 가능
    public PaymentResponse createPayment(CreatePaymentRequest request, Long sponsorId) {
        validateSponsorOwnsFunding(request.fundingId(), sponsorId);

        CreatedPayment created = selfProvider.getObject().createPendingPayment(request);

        try {
            PaymentSessionResult session = paymentGateway.createSession(
                    created.orderId(),
                    created.amount(),
                    created.method()
            );

            PaymentResponse.VbankInfo vbankInfo = null;
            if (created.method() == PaymentMethod.VIRTUAL_ACCOUNT
                    && paymentGateway.issuesVirtualAccountAtCreateTime()) {
                VirtualAccountIssueResult virtualAccount = selfProvider.getObject()
                        .issueAndSaveVirtualAccount(created.orderId(), created.amount());
                selfProvider.getObject().saveVbankDeposit(created.id(), virtualAccount);
                vbankInfo = new PaymentResponse.VbankInfo(
                        virtualAccount.bankCode(),
                        virtualAccount.accountNumber(),
                        virtualAccount.dueDate()
                );
            }

            return toResponse(created, session.clientKey(), session.redirectUrl(), vbankInfo);
        } catch (RuntimeException e) {
            selfProvider.getObject().failPayment(created.id());
            throw e;
        }
    }

    // PENDING 결제 레코드 생성 — Funding 락 후 중복 결제 방지
    @Transactional
    public CreatedPayment createPendingPayment(CreatePaymentRequest request) {
        Funding funding = fundingRepository.findByIdForUpdate(request.fundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        validateFundingPayable(funding, request.amount());
        validateNoSuccessfulPayment(request.fundingId());
        validateNoPendingPayment(request.fundingId());

        Payment payment = paymentRepository.save(Payment.createPending(
                request.fundingId(),
                generateOrderId(request.fundingId()),
                request.amount(),
                request.method()
        ));

        return CreatedPayment.from(payment);
    }

    // ── 카드 결제 승인 ─────────────────────────────────────────────────────

    // PG confirm 후 Payment SUCCESS + Funding PAID 처리 (카드) 또는 가상계좌 발급 대기 — 후원자 본인만
    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request, Long sponsorId) {
        validateSponsorOwnsPayment(paymentId, sponsorId);

        ConfirmPrepare prepare = selfProvider.getObject().prepareConfirm(paymentId, request.amount());

        PaymentConfirmResult result = paymentGateway.confirm(
                request.paymentKey(),
                prepare.orderId(),
                request.amount()
        );

        if (!result.success()) {
            selfProvider.getObject().failPayment(paymentId);
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        if (prepare.method() == PaymentMethod.VIRTUAL_ACCOUNT) {
            return selfProvider.getObject().completeVirtualAccountConfirm(paymentId, result);
        }

        Payment payment = selfProvider.getObject().completeCardPayment(
                paymentId,
                result.paymentKey(),
                request.amount()
        );
        return toResponse(payment, null, null, null);
    }

    @Transactional
    public PaymentResponse completeVirtualAccountConfirm(Long paymentId, PaymentConfirmResult result) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentConfirmable(payment, payment.getAmount());

        if (!result.awaitingDeposit() || result.virtualAccount() == null) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        payment.registerVirtualAccountPending(result.paymentKey(), result.tossWebhookSecret());

        VirtualAccountIssueResult issued = selfProvider.getObject()
                .issueAndSaveVirtualAccount(payment.getOrderId(), payment.getAmount(), result.virtualAccount());
        selfProvider.getObject().saveVbankDeposit(payment.getId(), issued);

        PaymentResponse.VbankInfo vbankInfo = new PaymentResponse.VbankInfo(
                issued.bankCode(),
                issued.accountNumber(),
                issued.dueDate()
        );
        return toResponse(payment, null, null, vbankInfo);
    }

    @Transactional(readOnly = true)
    public ConfirmPrepare prepareConfirm(Long paymentId, Long amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentConfirmable(payment, amount);
        return ConfirmPrepare.from(payment);
    }

    @Transactional
    public Payment completeCardPayment(Long paymentId, String paymentKey, Long amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentConfirmable(payment, amount);

        Funding funding = fundingRepository.findByIdForUpdate(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        validateFundingPayable(funding, amount);
        validateNoSuccessfulPayment(payment.getFundingId());

        payment.complete(paymentKey);
        funding.markAsPaid();
        publishFundingPaidEvent(payment.getFundingId());
        return payment;
    }

    // ── 가상계좌 ───────────────────────────────────────────────────────────

    // PG 가상계좌 발급 후 VirtualAccount 저장
    @Transactional
    public VirtualAccountIssueResult issueAndSaveVirtualAccount(String orderId, long amount) {
        VirtualAccountIssueResult issued = paymentGateway.issueVirtualAccount(orderId, amount);

        VirtualAccount saved = virtualAccountRepository.save(VirtualAccount.create(
                orderId,
                issued.bankCode(),
                issued.accountNumber(),
                issued.dueDate(),
                amount
        ));

        return new VirtualAccountIssueResult(
                saved.getId(),
                saved.getBankCode(),
                saved.getAccountNumber(),
                saved.getDueDate()
        );
    }

    // 가상계좌 입금 대기 레코드 저장
    @Transactional
    public void saveVbankDeposit(Long paymentId, VirtualAccountIssueResult virtualAccount) {
        vbankDepositRepository.save(VbankDeposit.createWaiting(
                paymentId,
                virtualAccount.virtualAccountId(),
                virtualAccount.bankCode(),
                virtualAccount.accountNumber(),
                virtualAccount.dueDate()
        ));
    }

    // 가상계좌 입금 완료 웹훅 — PG 검증 후 DB 반영 (멱등)
    public void processDepositWebhook(
            String orderId,
            Long amount,
            String providedSecret,
            String eventId,
            String tossWebhookSecret
    ) {
        verifyWebhookAccess(providedSecret, orderId, tossWebhookSecret);

        String resolvedEventId = resolveEventId(eventId, orderId, amount);
        if (paymentWebhookLogRepository.existsByEventId(resolvedEventId)) {
            return;
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        Long verifyAmount = amount != null ? amount : payment.getAmount();
        PaymentVerifyResult verifyResult = paymentGateway.verifyVirtualAccountDeposit(orderId, verifyAmount);
        if (!verifyResult.verified()) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        boolean completed = selfProvider.getObject().completeDepositWebhook(orderId, verifyAmount);
        if (!completed) {
            return;
        }
    }

    /** 테스트·레거시 웹훅 호출용 */
    @Deprecated
    public void processDepositWebhook(String orderId, Long amount, String providedSecret, String eventId) {
        processDepositWebhook(orderId, amount, providedSecret, eventId, null);
    }

    @Transactional
    public boolean completeDepositWebhook(String orderId, Long amount) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return false;
        }

        if (payment.getMethod() != PaymentMethod.VIRTUAL_ACCOUNT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        validatePaymentConfirmable(payment, amount);

        Funding funding = fundingRepository.findByIdForUpdate(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        validateFundingPayable(funding, amount);
        validateNoSuccessfulPayment(payment.getFundingId());

        VbankDeposit vbankDeposit = vbankDepositRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        vbankDeposit.markDeposited();

        payment.completeIfPending();
        funding.markAsPaid();
        publishFundingPaidEvent(payment.getFundingId());
        return true;
    }

    @Transactional
    public VirtualAccountIssueResult issueAndSaveVirtualAccount(
            String orderId,
            long amount,
            VirtualAccountIssueResult issued
    ) {
        VirtualAccount saved = virtualAccountRepository.save(VirtualAccount.create(
                orderId,
                issued.bankCode(),
                issued.accountNumber(),
                issued.dueDate(),
                amount
        ));

        return new VirtualAccountIssueResult(
                saved.getId(),
                saved.getBankCode(),
                saved.getAccountNumber(),
                saved.getDueDate()
        );
    }

    // ── 조회 ──────────────────────────────────────────────────────────────

    // 내 결제 내역 조회 (스폰서 본인, 페이징)
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Long sponsorId, Pageable pageable) {
        return paymentRepository.findBySponsorId(sponsorId, pageable)
                .map(payment -> toResponse(payment, null, null, resolveVbankInfo(payment.getId())));
    }

    // 결제 단건 조회 — 후원자 본인 또는 ADMIN
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId, Long requesterId, Role requesterRole) {
        if (requesterRole != Role.ADMIN) {
            validateSponsorOwnsPayment(paymentId, requesterId);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        return toResponse(payment, null, null, resolveVbankInfo(payment.getId()));
    }

    // 환불 요청 — Payment/Funding REFUNDED + Idea 누적 후원금 차감 (후원자 본인)
    @Transactional
    public void refundPayment(Long paymentId, Long sponsorId) {
        validateSponsorOwnsPayment(paymentId, sponsorId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        Funding funding = fundingRepository.findByIdForUpdate(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        if (funding.getStatus() != FundingStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        funding.markAsRefunded();
        payment.markAsRefunded();

        ideaRepository.findByIdForUpdate(funding.getIdeaId())
                .ifPresent(idea -> idea.subtractFundingAmount(funding.getAmount()));
    }

    // 결제 실패 처리 — PG 오류·가상계좌 만료 시 호출
    @Transactional
    public void failPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.fail();
        cancelFundingIfPending(payment.getFundingId());
    }

    private void cancelFundingIfPending(Long fundingId) {
        fundingRepository.findById(fundingId).ifPresent(funding -> {
            if (funding.getStatus() == FundingStatus.PENDING_PAYMENT) {
                funding.markAsCancelled();
            }
        });
    }

    /**
     * 결제가 속한 후원(Funding)의 후원자와 요청자가 일치하는지 검증합니다.
     * 타인 결제의 생성·승인·조회를 차단합니다.
     */
    private void validateSponsorOwnsPayment(Long paymentId, Long sponsorId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        validateSponsorOwnsFunding(payment.getFundingId(), sponsorId);
    }

    private void validateSponsorOwnsFunding(Long fundingId, Long sponsorId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));
        if (!funding.getSponsorId().equals(sponsorId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private void publishFundingPaidEvent(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        if (funding.getStatus() != FundingStatus.PAID) {
            return;
        }

        eventPublisher.publishEvent(new FundingPaidEvent(
                funding.getId(),
                funding.getIdeaId(),
                funding.getSponsorId(),
                funding.getAmount()
        ));
    }

    private String resolveEventId(String eventId, String orderId, Long amount) {
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }
        return "auto-" + orderId + "-" + amount;
    }

    private void verifyWebhookAccess(String providedSecret, String orderId, String tossWebhookSecret) {
        if (providedSecret != null && providedSecret.equals(paymentProperties.webhook().secret())) {
            return;
        }

        if (tossWebhookSecret == null || tossWebhookSecret.isBlank()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getTossWebhookSecret() == null
                || !payment.getTossWebhookSecret().equals(tossWebhookSecret)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    private void verifyWebhookSecret(String providedSecret) {
        String webhookSecret = paymentProperties.webhook().secret();
        if (providedSecret == null || !webhookSecret.equals(providedSecret)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    public void verifyWebhookSecretOnly(String providedSecret) {
        if (providedSecret != null && providedSecret.equals(paymentProperties.webhook().secret())) {
            return;
        }
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    public void verifyWebhookSecretOnly(String providedSecret, String orderId, String tossWebhookSecret) {
        try {
            verifyWebhookAccess(providedSecret, orderId, tossWebhookSecret);
        } catch (CustomException ignored) {
            if (providedSecret == null || !providedSecret.equals(paymentProperties.webhook().secret())) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }
    }

    private PaymentResponse.VbankInfo resolveVbankInfo(Long paymentId) {
        return vbankDepositRepository.findByPaymentId(paymentId)
                .map(vbank -> new PaymentResponse.VbankInfo(
                        vbank.getBankCode(),
                        vbank.getAccountNumber(),
                        vbank.getDueDate()
                ))
                .orElse(null);
    }

    private void validateFundingPayable(Funding funding, Long amount) {
        if (funding.getStatus() != FundingStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        if (!funding.getAmount().equals(amount)) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void validateNoSuccessfulPayment(Long fundingId) {
        if (paymentRepository.existsByFundingIdAndStatus(fundingId, PaymentStatus.SUCCESS)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_DONE);
        }
    }

    private void validateNoPendingPayment(Long fundingId) {
        if (paymentRepository.existsByFundingIdAndStatus(fundingId, PaymentStatus.PENDING)) {
            throw new CustomException(ErrorCode.FUNDING_DUPLICATE_PAYMENT);
        }
    }

    private void validatePaymentConfirmable(Payment payment, Long amount) {
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_DONE);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        if (!payment.getAmount().equals(amount)) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private String generateOrderId(Long fundingId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "order-" + fundingId + "-" + suffix;
    }

    private PaymentResponse toResponse(
            Payment payment,
            String clientKey,
            String redirectUrl,
            PaymentResponse.VbankInfo vbankInfo
    ) {
        return new PaymentResponse(
                payment.getId(),
                payment.getFundingId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getApprovedAt(),
                payment.getCreatedAt(),
                clientKey,
                redirectUrl,
                vbankInfo
        );
    }

    private PaymentResponse toResponse(
            CreatedPayment created,
            String clientKey,
            String redirectUrl,
            PaymentResponse.VbankInfo vbankInfo
    ) {
        return new PaymentResponse(
                created.id(),
                created.fundingId(),
                created.orderId(),
                created.amount(),
                PaymentStatus.PENDING,
                created.method(),
                null,
                created.createdAt(),
                clientKey,
                redirectUrl,
                vbankInfo
        );
    }
}
