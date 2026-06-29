package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Deposit;
import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.event.FundingPaidEvent;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.dto.request.ConfirmPaymentRequest;
import com.team04.domain.payment.dto.request.CreatePaymentRequest;
import com.team04.domain.payment.dto.response.ConfirmPrepare;
import com.team04.domain.payment.dto.response.CreatedPayment;
import com.team04.domain.payment.dto.response.PaymentConfigResponse;
import com.team04.domain.payment.dto.response.PaymentConfirmResult;
import com.team04.domain.payment.dto.response.PaymentRefundResult;
import com.team04.domain.payment.dto.response.PaymentResponse;
import com.team04.domain.payment.dto.response.PaymentSessionResult;
import com.team04.domain.payment.dto.response.PaymentVerifyResult;
import com.team04.domain.payment.dto.response.VirtualAccountIssueResult;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.entity.PaymentWebhookLog;
import com.team04.domain.payment.entity.VbankLedgerType;
import com.team04.domain.payment.entity.VirtualAccount;
import com.team04.domain.payment.entity.VbankDeposit;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.payment.repository.PaymentWebhookLogRepository;
import com.team04.domain.payment.repository.VirtualAccountRepository;
import com.team04.domain.payment.repository.VbankDepositRepository;
import com.team04.domain.payment.service.IdeaVbankPoolService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import com.team04.domain.user.entity.Role;
import com.team04.global.config.payment.PaymentProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final DepositRepository depositRepository;
    private final IdeaRepository ideaRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<PaymentService> selfProvider;
    private final PaymentProperties paymentProperties;
    private final IdeaVbankPoolService ideaVbankPoolService;
    private final VbankLedgerService vbankLedgerService;

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
            if (created.method() == PaymentMethod.VIRTUAL_ACCOUNT) {
                vbankInfo = resolveVbankInfoForCreate(created);
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

    /**
     * 제안자 보증금 PG 결제 세션 생성.
     * confirm/웹훅 성공 시 {@link #finalizeDepositPayment(Payment)} 로 Deposit HELD 확정.
     */
    @Transactional
    public PaymentResponse createDepositPayment(
            Long ideaId,
            Long proposerId,
            Long amount,
            PaymentMethod method
    ) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.validateOwner(proposerId);

        if (depositRepository.existsByIdeaIdAndStatus(
                ideaId, com.team04.domain.funding.entity.FundingTypes.DepositStatus.HELD)) {
            throw new CustomException(ErrorCode.PROJECT_FEE_ALREADY_PAID);
        }
        if (idea.getDepositAmount() != null
                && idea.getDepositAmount() > 0
                && !idea.getDepositAmount().equals(amount)) {
            throw new CustomException(ErrorCode.DEPOSIT_AMOUNT_MISMATCH);
        }

        String orderId = "deposit-" + ideaId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Payment payment = paymentRepository.save(Payment.createDepositPending(ideaId, orderId, amount, method));

        try {
            PaymentSessionResult session = paymentGateway.createSession(orderId, amount, method);
            PaymentResponse.VbankInfo vbankInfo = null;
            if (method == PaymentMethod.VIRTUAL_ACCOUNT
                    && paymentGateway.issuesVirtualAccountAtCreateTime()) {
                VirtualAccountIssueResult virtualAccount = selfProvider.getObject()
                        .issueAndSaveVirtualAccount(orderId, amount);
                selfProvider.getObject().saveVbankDeposit(payment.getId(), virtualAccount);
                vbankInfo = new PaymentResponse.VbankInfo(
                        virtualAccount.bankCode(),
                        virtualAccount.accountNumber(),
                        virtualAccount.dueDate()
                );
            }
            return toResponse(payment, session.clientKey(), session.redirectUrl(), vbankInfo);
        } catch (RuntimeException e) {
            payment.fail();
            throw e;
        }
    }

    // ── 시연용 데모 결제 ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentConfigResponse getPaymentConfig() {
        PaymentSessionResult session = paymentGateway.createSession("config", 1L, PaymentMethod.CARD);
        return new PaymentConfigResponse(
                paymentProperties.demo().enabled(),
                session.clientKey(),
                paymentProperties.gateway().type()
        );
    }

    /**
     * 포트폴리오 시연 — 토스 테스트 결제창 대체. 아무 값이나 입력해도 즉시 결제 완료.
     */
    public PaymentResponse demoConfirmPayment(Long paymentId, Long userId) {
        if (!paymentProperties.demo().enabled()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.isDepositPayment()) {
            validateProposerOwnsDepositPayment(payment, userId);
        } else {
            validateSponsorOwnsPayment(paymentId, userId);
        }

        String demoKey = "demo-" + UUID.randomUUID();

        if (payment.getMethod() == PaymentMethod.CARD) {
            if (payment.isDepositPayment()) {
                Payment completed = selfProvider.getObject().completeDepositCardPayment(
                        paymentId, demoKey, payment.getAmount());
                return toResponse(completed, null, null, null);
            }
            Payment completed = selfProvider.getObject().completeCardPayment(
                    paymentId, demoKey, payment.getAmount());
            return toResponse(completed, null, null, null);
        }

        if (payment.getMethod() == PaymentMethod.VIRTUAL_ACCOUNT) {
            if (!vbankDepositRepository.findByPaymentId(paymentId).isPresent()) {
                VirtualAccountIssueResult issued = paymentGateway.issueVirtualAccount(
                        payment.getOrderId(), payment.getAmount());
                selfProvider.getObject().issueAndSaveVirtualAccount(
                        payment.getOrderId(), payment.getAmount(), issued);
                selfProvider.getObject().saveVbankDeposit(paymentId, issued);
            }
            selfProvider.getObject().completeDepositWebhook(payment.getOrderId(), payment.getAmount());
            Payment updated = paymentRepository.findById(paymentId).orElseThrow();
            return toResponse(updated, null, null, resolveVbankInfo(paymentId));
        }

        throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
    }

    // ── 카드 결제 승인 ─────────────────────────────────────────────────────

    // PG confirm 후 Payment SUCCESS + Funding PAID 처리 (카드) 또는 가상계좌 발급 대기 — 후원자 본인만
    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request, Long sponsorId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.isDepositPayment()) {
            validateProposerOwnsDepositPayment(payment, sponsorId);
        } else {
            validateSponsorOwnsPayment(paymentId, sponsorId);
        }

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

        if (payment.isDepositPayment()) {
            Payment completed = selfProvider.getObject().completeDepositCardPayment(
                    paymentId, result.paymentKey(), request.amount());
            return toResponse(completed, null, null, null);
        }

        Payment sponsorshipPayment = selfProvider.getObject().completeCardPayment(
                paymentId,
                result.paymentKey(),
                request.amount()
        );
        return toResponse(sponsorshipPayment, null, null, null);
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

        if (!payment.isDepositPayment()) {
            Funding funding = fundingRepository.findById(payment.getFundingId())
                    .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));
            ideaVbankPoolService.registerPoolFromConfirm(
                    funding.getIdeaId(), payment.getOrderId(), issued);
        }

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
        Payment paymentRef = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (paymentRef.isDepositPayment()) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        Funding funding = fundingRepository.findByIdForUpdate(paymentRef.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentConfirmable(payment, amount);
        validateFundingPayable(funding, amount);
        validateNoSuccessfulPayment(payment.getFundingId());

        payment.complete(paymentKey);
        funding.markAsPaid();
        // 후원 결제 성공 금액은 아이디어 가상계좌의 실제 입금으로 장부에 남긴다.
        vbankLedgerService.recordIn(
                funding.getIdeaId(),
                VbankLedgerType.FUNDING_PAID,
                payment.getAmount(),
                "payment-" + payment.getId() + "-FUNDING-PAID",
                "Payment",
                payment.getId(),
                "후원금 입금"
        );
        publishFundingPaidEvent(payment.getFundingId());
        return payment;
    }

    /** 보증금 카드 결제 승인 완료 */
    @Transactional
    public Payment completeDepositCardPayment(Long paymentId, String paymentKey, Long amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.isDepositPayment()) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        validatePaymentConfirmable(payment, amount);
        payment.complete(paymentKey);
        finalizeDepositPayment(payment);
        return payment;
    }

    /** 보증금 결제 SUCCESS 후 Deposit HELD 생성 */
    @Transactional
    public void finalizeDepositPayment(Payment payment) {
        if (!payment.isDepositPayment() || payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        Long ideaId = payment.getIdeaId();
        if (depositRepository.existsByIdeaIdAndStatus(
                ideaId, com.team04.domain.funding.entity.FundingTypes.DepositStatus.HELD)) {
            return;
        }
        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        Deposit deposit = depositRepository.save(Deposit.createHeld(ideaId, idea.getUserId(), payment.getAmount(), payment.getId()));
        // 보증금 결제 성공 금액은 아이디어 가상계좌의 실제 입금으로 장부에 남긴다.
        vbankLedgerService.recordIn(
                ideaId,
                VbankLedgerType.DEPOSIT_PAID,
                payment.getAmount(),
                "payment-" + payment.getId() + "-DEPOSIT-PAID",
                "Deposit",
                deposit.getId(),
                "보증금 입금"
        );
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
        selfProvider.getObject().processDepositWebhookTransactional(
                orderId, amount, providedSecret, eventId, tossWebhookSecret);
    }

    @Transactional
    public void processDepositWebhookTransactional(
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

        PaymentWebhookLog webhookLog = claimWebhookLog(resolvedEventId, orderId, amount);
        if (webhookLog == null) {
            return;
        }

        try {
            Payment payment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

            Long verifyAmount = amount != null ? amount : payment.getAmount();
            PaymentVerifyResult verifyResult = paymentGateway.verifyVirtualAccountDeposit(orderId, verifyAmount);
            if (!verifyResult.verified()) {
                webhookLog.markFailed();
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }

            completeDepositWebhook(orderId, verifyAmount);
            webhookLog.markDone();
        } catch (RuntimeException e) {
            webhookLog.markFailed();
            throw e;
        }
    }

    /** 테스트·레거시 웹훅 호출용 */
    @Deprecated
    public void processDepositWebhook(String orderId, Long amount, String providedSecret, String eventId) {
        processDepositWebhook(orderId, amount, providedSecret, eventId, null);
    }

    @Transactional
    public boolean completeDepositWebhook(String orderId, Long amount) {
        Payment paymentRef = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (paymentRef.getStatus() == PaymentStatus.SUCCESS) {
            return false;
        }

        if (paymentRef.getMethod() != PaymentMethod.VIRTUAL_ACCOUNT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        if (paymentRef.isDepositPayment()) {
            Payment payment = paymentRepository.findByIdForUpdate(paymentRef.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
            validatePaymentConfirmable(payment, amount);

            VbankDeposit vbankDeposit = vbankDepositRepository.findByPaymentId(payment.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
            vbankDeposit.markDeposited();
            payment.completeIfPending();
            finalizeDepositPayment(payment);
            return true;
        }

        Funding funding = fundingRepository.findByIdForUpdate(paymentRef.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        Payment payment = paymentRepository.findByIdForUpdate(paymentRef.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentConfirmable(payment, amount);
        validateNoOtherSuccessfulPayment(payment.getFundingId(), payment.getId());
        validateFundingPayable(funding, amount);

        VbankDeposit vbankDeposit = vbankDepositRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        vbankDeposit.markDeposited();
        payment.completeIfPending();

        funding.markAsPaid();
        // 가상계좌 후원 입금 웹훅 성공 시에도 카드 결제와 동일하게 실제 입금 장부를 남긴다.
        vbankLedgerService.recordIn(
                funding.getIdeaId(),
                VbankLedgerType.FUNDING_PAID,
                payment.getAmount(),
                "payment-" + payment.getId() + "-FUNDING-PAID",
                "Payment",
                payment.getId(),
                "후원금 입금"
        );
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
            Payment paymentForAuth = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
            if (paymentForAuth.isDepositPayment()) {
                validateProposerOwnsDepositPayment(paymentForAuth, requesterId);
            } else {
                validateSponsorOwnsPayment(paymentId, requesterId);
            }
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        return toResponse(payment, null, null, resolveVbankInfo(payment.getId()));
    }

    // 환불 요청 — Payment/Funding REFUNDED + Idea 누적 후원금 차감 (후원자 본인)
    @Transactional
    public void refundPayment(Long paymentId, Long sponsorId) {
        Long fundingId = paymentRepository.findFundingIdById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        Long ideaId = fundingRepository.findIdeaIdById(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        // 관계 ID만 먼저 읽고 엔티티는 락 조회로만 올린다.
        // 일반 조회 엔티티가 영속성 컨텍스트에 남으면 락 대기 후에도 오래된 SUCCESS/PAID 상태로 중복 환불될 수 있다.
        Idea idea = ideaRepository.findByIdForUpdate(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        Funding funding = fundingRepository.findByIdForUpdate(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!funding.getSponsorId().equals(sponsorId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        if (funding.getStatus() != FundingStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        // 실제 PG 환불을 호출하기 전에 가상계좌 장부 출금 가능 여부를 먼저 검증한다.
        vbankLedgerService.validateSufficientBalanceForOut(funding.getIdeaId(), payment.getAmount());

        // PG 환불 API 호출 후 DB 반영 — 실패 시 상태 변경하지 않음
        PaymentRefundResult refundResult = paymentGateway.refund(
                payment.getPaymentKey(),
                payment.getOrderId(),
                payment.getAmount(),
                "후원 취소"
        );
        if (!refundResult.success()) {
            throw new CustomException(ErrorCode.REFUND_FAILED);
        }

        funding.markAsRefunded();
        payment.markAsRefunded();

        idea.subtractFundingAmount(funding.getAmount());
        // 후원자 직접 취소 환불은 실제 출금으로 보아 가상계좌 장부 잔액에서 차감한다.
        vbankLedgerService.recordOut(
                funding.getIdeaId(),
                VbankLedgerType.SPONSOR_REFUND_PAID,
                payment.getAmount(),
                "payment-" + payment.getId() + "-SPONSOR-REFUND",
                "Payment",
                payment.getId(),
                "후원자 직접 취소 환불"
        );
    }

    // 결제 실패 처리 — PG 오류·가상계좌 만료 시 호출
    @Transactional
    public void failPayment(Long paymentId) {
        Payment paymentRef = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (paymentRef.getFundingId() != null) {
            fundingRepository.findByIdForUpdate(paymentRef.getFundingId())
                    .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));
        }

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.fail();
        vbankDepositRepository.findByPaymentId(paymentId)
                .ifPresent(VbankDeposit::markCanceled);
        if (payment.getFundingId() != null) {
            cancelFundingIfPending(payment.getFundingId());
        }
    }

    private void cancelFundingIfPending(Long fundingId) {
        fundingRepository.findByIdForUpdate(fundingId).ifPresent(funding -> {
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
        if (fundingId == null) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));
        if (!funding.getSponsorId().equals(sponsorId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    /** 보증금 결제는 아이디어 제안자 본인만 승인할 수 있습니다. */
    private void validateProposerOwnsDepositPayment(Payment payment, Long userId) {
        if (!payment.isDepositPayment() || payment.getIdeaId() == null) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(payment.getIdeaId())
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.validateOwner(userId);
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

    private PaymentWebhookLog claimWebhookLog(String eventId, String orderId, Long amount) {
        try {
            return paymentWebhookLogRepository.save(PaymentWebhookLog.create(
                    eventId,
                    orderId,
                    "PROCESSING",
                    amount != null ? amount : 0L,
                    "TOSS"
            ));
        } catch (DataIntegrityViolationException e) {
            return null;
        }
    }

    private PaymentResponse.VbankInfo resolveVbankInfoForCreate(CreatedPayment created) {
        if (ideaVbankPoolService.usePoolForCreateTimeVbank()) {
            Funding funding = fundingRepository.findById(created.fundingId())
                    .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));
            var pool = ideaVbankPoolService.ensurePoolForIdea(funding.getIdeaId());
            VirtualAccountIssueResult virtualAccount = ideaVbankPoolService.toIssueResult(pool);
            selfProvider.getObject().saveVbankDeposit(created.id(), virtualAccount);
            return new PaymentResponse.VbankInfo(
                    virtualAccount.bankCode(),
                    virtualAccount.accountNumber(),
                    virtualAccount.dueDate()
            );
        }

        if (paymentGateway.issuesVirtualAccountAtCreateTime()) {
            VirtualAccountIssueResult virtualAccount = selfProvider.getObject()
                    .issueAndSaveVirtualAccount(created.orderId(), created.amount());
            selfProvider.getObject().saveVbankDeposit(created.id(), virtualAccount);
            return new PaymentResponse.VbankInfo(
                    virtualAccount.bankCode(),
                    virtualAccount.accountNumber(),
                    virtualAccount.dueDate()
            );
        }
        return null;
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

    private void validateNoOtherSuccessfulPayment(Long fundingId, Long currentPaymentId) {
        if (paymentRepository.existsByFundingIdAndStatusAndIdNot(
                fundingId, PaymentStatus.SUCCESS, currentPaymentId)) {
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
        Long ideaId = null;
        String ideaTitle = null;
        if (payment.getFundingId() != null) {
            ideaId = fundingRepository.findById(payment.getFundingId())
                    .map(com.team04.domain.funding.entity.Funding::getIdeaId)
                    .orElse(null);
        } else if (payment.getIdeaId() != null) {
            ideaId = payment.getIdeaId();
        }
        if (ideaId != null) {
            final Long id = ideaId;
            ideaTitle = ideaRepository.findById(id)
                    .map(com.team04.domain.idea.entity.Idea::getTitle)
                    .orElse(null);
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getFundingId(),
                ideaId,
                ideaTitle,
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
                null,
                null,
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
