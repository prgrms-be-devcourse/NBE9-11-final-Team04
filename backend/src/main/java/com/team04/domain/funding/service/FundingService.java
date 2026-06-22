package com.team04.domain.funding.service;

import com.team04.domain.funding.dto.request.CreateFundingRequest;
import com.team04.domain.funding.dto.response.CreateFundingResponse;
import com.team04.domain.funding.dto.response.FundingResponse;
import com.team04.domain.funding.entity.Funding;
import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.milestone.entity.Milestone;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.payment.dto.request.CreatePaymentRequest;
import com.team04.domain.payment.dto.response.PaymentResponse;
import com.team04.domain.payment.service.PaymentService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final IdeaRepository ideaRepository;
    private final MilestoneRepository milestoneRepository;
    private final PaymentService paymentService;

    public CreateFundingResponse createFunding(Long ideaId, Long sponsorId, CreateFundingRequest request) {
        Idea idea = validateFundableIdea(ideaId, sponsorId, request.amount());
        int milestoneStep = resolveMilestoneStep(ideaId);

        Funding funding = fundingRepository.save(Funding.createPending(
                ideaId,
                sponsorId,
                milestoneStep,
                request.amount(),
                idea.getRewardType()
        ));

        PaymentResponse payment = paymentService.createPayment(new CreatePaymentRequest(
                funding.getId(),
                request.amount(),
                request.paymentMethod()
        ));

        return CreateFundingResponse.from(funding, payment);
    }

    @Transactional(readOnly = true)
    public FundingResponse getFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new CustomException(ErrorCode.FUNDING_NOT_FOUND));

        return toResponse(funding);
    }

    @Transactional(readOnly = true)
    public Page<FundingResponse> getFundingsByIdea(Long ideaId, Pageable pageable) {
        return fundingRepository.findByIdeaIdOrderByCreatedAtDesc(ideaId, pageable)
                .map(this::toResponse);
    }

    private int resolveMilestoneStep(Long ideaId) {
        return milestoneRepository.findByIdeaIdAndStatus(ideaId, MilestoneStatus.IN_PROGRESS)
                .map(Milestone::getStep)
                .orElseGet(() -> milestoneRepository.findByIdeaIdOrderByStep(ideaId).stream()
                        .filter(milestone -> milestone.getStatus() == MilestoneStatus.PENDING)
                        .mapToInt(Milestone::getStep)
                        .max()
                        .orElse(0));
    }

    private Idea validateFundableIdea(Long ideaId, Long sponsorId, Long amount) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        if (idea.getStatus() != IdeaStatus.OPEN && idea.getStatus() != IdeaStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.IDEA_NOT_OPEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(idea.getFundingStartAt()) || now.isAfter(idea.getFundingEndAt())) {
            throw new CustomException(ErrorCode.FUNDING_ALREADY_CLOSED);
        }

        if (idea.getUserId().equals(sponsorId)) {
            throw new CustomException(ErrorCode.IDEA_SELF_FUNDING_NOT_ALLOWED);
        }

        if (amount == null || amount < 1) {
            throw new CustomException(ErrorCode.INVALID_FUNDING_AMOUNT);
        }

        return idea;
    }

    private FundingResponse toResponse(Funding funding) {
        return new FundingResponse(
                funding.getId(),
                funding.getIdeaId(),
                funding.getSponsorId(),
                funding.getMilestoneStep(),
                funding.getAmount(),
                funding.getRewardType().name(),
                funding.getStatus().name(),
                funding.getCreatedAt(),
                funding.getRefundedAt()
        );
    }
}
