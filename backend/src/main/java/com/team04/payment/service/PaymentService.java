package com.team04.payment.service;

import com.team04.payment.dto.request.ConfirmPaymentRequest;
import com.team04.payment.dto.request.CreatePaymentRequest;
import com.team04.payment.dto.response.PaymentResponse;
import com.team04.payment.entity.Payment;
import com.team04.payment.repository.PaymentRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // TODO: reference 검증, orderId 서버 발급, PG 연동
        throw new UnsupportedOperationException("결제 생성 로직 구현 예정");
    }

    @Transactional
    public PaymentResponse confirmPayment(Long paymentId, ConfirmPaymentRequest request) {
        // TODO: PG confirm API 호출, 금액 검증, Payment DONE 처리
        throw new UnsupportedOperationException("결제 승인 로직 구현 예정");
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        return toResponse(payment);
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
