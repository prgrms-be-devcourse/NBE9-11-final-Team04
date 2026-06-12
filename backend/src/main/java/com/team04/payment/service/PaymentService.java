package com.team04.payment.service;

import com.team04.funding.entity.Funding;
import com.team04.funding.entity.FundingTypes.FundingStatus;
import com.team04.funding.repository.FundingRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.payment.dto.request.ConfirmPaymentRequest;
import com.team04.payment.dto.request.CreatePaymentRequest;
import com.team04.payment.dto.response.PaymentResponse;
import com.team04.payment.entity.Payment;
import com.team04.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FundingRepository fundingRepository;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Funding funding = fundingRepository.findByIdForUpdate(request.fundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        validateFundingPayable(funding, request.amount());
        validateNoSuccessfulPayment(request.fundingId());
        validateNoPendingPayment(request.fundingId());

        Payment payment = Payment.createPending(
                request.fundingId(),
                generateOrderId(request.fundingId()),
                request.amount(),
                request.method()
        );

        // TODO: PG 결제 세션 생성 API 연동
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validatePaymentConfirmable(payment, request.amount());

        Funding funding = fundingRepository.findByIdForUpdate(payment.getFundingId())
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        validateFundingPayable(funding, request.amount());
        validateNoSuccessfulPayment(payment.getFundingId());

        // TODO: PG confirm API 호출 및 응답 검증
        payment.complete(request.paymentKey());
        funding.markAsPaid();

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        return toResponse(payment);
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

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getFundingId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getApprovedAt(),
                payment.getCreatedAt()
        );
    }
}
