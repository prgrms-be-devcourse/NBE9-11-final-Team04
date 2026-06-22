package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.event.FundingPaidEvent;
import com.team04.domain.funding.repository.FundingRepository;
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
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.payment.repository.PaymentWebhookLogRepository;
import com.team04.domain.payment.repository.VbankDepositRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final VbankDepositRepository vbankDepositRepository;
    private final PaymentWebhookLogRepository paymentWebhookLogRepository;
    private final FundingRepository fundingRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentTxService paymentTxService;
    private final VirtualAccountService virtualAccountService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${payment.webhook.secret:dev-webhook-secret}")
    private String webhookSecret;

    public PaymentResponse createPayment(CreatePaymentRequest request) {
        CreatedPayment created = paymentTxService.createPendingPayment(request);

        try {
            PaymentSessionResult session = paymentGateway.createSession(
                    created.orderId(),
                    created.amount(),
                    created.method()
            );

            PaymentResponse.VbankInfo vbankInfo = null;
            if (created.method() == PaymentMethod.VIRTUAL_ACCOUNT) {
                VirtualAccountIssueResult virtualAccount = virtualAccountService.issueAndSave(
                        created.orderId(),
                        created.amount()
                );
                paymentTxService.saveVbankDeposit(created.id(), virtualAccount);
                vbankInfo = new PaymentResponse.VbankInfo(
                        virtualAccount.bankCode(),
                        virtualAccount.accountNumber(),
                        virtualAccount.dueDate()
                );
            }

            return toResponse(created, session.clientKey(), session.redirectUrl(), vbankInfo);
        } catch (RuntimeException e) {
            paymentTxService.failPayment(created.id());
            throw e;
        }
    }

    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request) {
        ConfirmPrepare prepare = paymentTxService.prepareConfirm(paymentId, request.amount());

        PaymentConfirmResult result = paymentGateway.confirm(
                request.paymentKey(),
                prepare.orderId(),
                request.amount()
        );

        if (!result.success()) {
            paymentTxService.failPayment(paymentId);
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        Payment payment = paymentTxService.completeCardPayment(
                paymentId,
                result.paymentKey(),
                request.amount()
        );
        publishFundingPaidEvent(payment.getFundingId());
        return toResponse(payment, null, null, null);
    }

    /** 가상계좌 입금 웹훅 — PG 검증 후 DB 반영 (멱등) */
    public void processDepositWebhook(String orderId, Long amount, String providedSecret, String eventId) {
        verifyWebhookSecret(providedSecret);

        String resolvedEventId = resolveEventId(eventId, orderId, amount);
        if (paymentWebhookLogRepository.existsByEventId(resolvedEventId)) {
            return;
        }

        PaymentVerifyResult verifyResult = paymentGateway.verifyVirtualAccountDeposit(orderId, amount);
        if (!verifyResult.verified()) {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        paymentTxService.completeDepositWebhook(orderId, amount);

        paymentWebhookLogRepository.save(PaymentWebhookLog.create(
                resolvedEventId,
                orderId,
                "DONE",
                amount,
                "toss"
        ));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        publishFundingPaidEvent(payment.getFundingId());
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        PaymentResponse.VbankInfo vbankInfo = vbankDepositRepository.findByPaymentId(payment.getId())
                .map(vbank -> new PaymentResponse.VbankInfo(
                        vbank.getBankCode(),
                        vbank.getAccountNumber(),
                        vbank.getDueDate()
                ))
                .orElse(null);

        return toResponse(payment, null, null, vbankInfo);
    }

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

    private void verifyWebhookSecret(String providedSecret) {
        if (providedSecret == null || !webhookSecret.equals(providedSecret)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
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
