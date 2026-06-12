package com.team04.funding.service;

import com.team04.funding.dto.request.CreateFundingRequest;
import com.team04.funding.dto.response.FundingResponse;
import com.team04.funding.entity.Funding;
import com.team04.funding.repository.FundingRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;

    @Transactional
    public FundingResponse createFunding(Long ideaId, CreateFundingRequest request) {
        // TODO: Idea OPEN 검증, 후원 가능 여부 확인
        // TODO: PaymentService.createPayment() 호출
        throw new UnsupportedOperationException("후원 생성 로직 구현 예정");
    }

    @Transactional(readOnly = true)
    public FundingResponse getFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        return toResponse(funding);
    }

    @Transactional(readOnly = true)
    public List<FundingResponse> getFundingsByIdea(Long ideaId) {
        return fundingRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FundingResponse toResponse(Funding funding) {
        return new FundingResponse(
                funding.getId(),
                funding.getIdeaId(),
                funding.getSponsorId(),
                funding.getAmount(),
                funding.getStatus().name(),
                funding.getCreatedAt()
        );
    }
}
