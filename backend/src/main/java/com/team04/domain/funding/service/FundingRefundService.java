package com.team04.domain.funding.service;

import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.payment.entity.Payment;
import com.team04.domain.payment.entity.PaymentTypes.PaymentStatus;
import com.team04.domain.payment.repository.PaymentRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FundingRefundService {

    private final FundingRepository fundingRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void refundFunding(Long fundingId) {
        Funding funding = fundingRepository.findByIdForUpdate(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        Payment payment = paymentRepository
                .findFirstByFundingIdAndStatusOrderByCreatedAtDesc(fundingId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        funding.markAsRefunded();
        payment.markAsRefunded();
    }
}
