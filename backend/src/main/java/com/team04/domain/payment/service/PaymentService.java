package com.team04.domain.payment.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.entity.FundingTypes.FundingStatus;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.payment.client.PaymentGateway;
import com.team04.domain.payment.client.dto.PaymentConfirmResult;
import com.team04.domain.payment.client.dto.PaymentSessionResult;
import com.team04.domain.payment.client.dto.VirtualAccountIssueResult;
import com.team04.domain.payment.dto.request.ConfirmPaymentRequest;
import com.team04.domain.payment.dto.request.CreatePaymentRequest;
import com.team04.domain.payment.dto.response.PaymentResponse;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentMethod;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.entity.VbankDeposit;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.domain.payment.repository.VbankDepositRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FundingRepository fundingRepository;
    private final VbankDepositRepository vbankDepositRepository;
    private final PaymentGateway paymentGateway;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
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

        PaymentSessionResult session = paymentGateway.createSession(
                payment.getOrderId(),
                payment.getAmount(),
                payment.getMethod()
        );

        PaymentResponse.VbankInfo vbankInfo = null;
        if (payment.getMethod() == PaymentMethod.VIRTUAL_ACCOUNT) {
            VirtualAccountIssueResult virtualAccount = paymentGateway.issueVirtualAccount(
                    payment.getOrderId(),
                    payment.getAmount()
            );
            vbankDepositRepository.save(VbankDeposit.createWaiting(
                    payment.getId(),
                    virtualAccount.virtualAccountId(),
                    virtualAccount.bankCode(),
                    virtualAccount.accountNumber(),
                    virtualAccount.dueDate()
            ));
            vbankInfo = new PaymentResponse.VbankInfo(
                    virtualAccount.bankCode(),
                    virtualAccount.accountNumber(),
                    virtualAccount.dueDate()
            );
        }

        return toResponse(payment, session.clientKey(), session.redirectUrl(), vbankInfo);
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getMethod() == PaymentMethod.VIRTUAL_ACCOUNT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_READY);
        }

        validatePaymentConfirmable(payment, request.amount());

        Funding funding = fundingRepository.findByIdForUpdate(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        validateFundingPayable(funding, request.amount());
        validateNoSuccessfulPayment(payment.getFundingId());

        PaymentConfirmResult result = paymentGateway.confirm(
                request.paymentKey(),
                payment.getOrderId(),
                request.amount()
        );
        if (!result.success()) {
            payment.fail();
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }

        completePayment(funding, payment, result.paymentKey());
        return toResponse(payment, null, null, null);
    }

    /** 가상계좌 입금 웹훅 — orderId 기준 멱등 처리 */
    @Transactional
    public void processDepositWebhook(String orderId, Long amount) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
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

        completePayment(funding, payment, "vbank-" + orderId);
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

    private void completePayment(Funding funding, Payment payment, String paymentKey) {
        payment.complete(paymentKey);
        funding.markAsPaid();
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
}
