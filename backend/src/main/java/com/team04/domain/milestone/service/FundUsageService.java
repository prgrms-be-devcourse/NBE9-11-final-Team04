package com.team04.domain.milestone.service;

import com.team04.domain.funding.repository.FundingRepository;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.dto.request.FundUsageRequest;
import com.team04.domain.milestone.dto.response.FundUsageResponse;
import com.team04.domain.milestone.entity.FundUsage;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.FundUsageRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.settlement.entity.PreSettlementStatus;
import com.team04.domain.settlement.repository.PreSettlementRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundUsageService {

    private final FundUsageRepository fundUsageRepository;
    private final MilestoneRepository milestoneRepository;
    private final PreSettlementRepository preSettlementRepository;
    private final FundingRepository fundingRepository;
    private final IdeaService ideaService;

    /**
     * 자금 사용 내역 입력 (Append Only)
     * 제안자만 가능, 본인 프로젝트인지 소유권 검증
     * 진행 중인 마일스톤이 있어야 함
     * 실제 지급받은 금액(COMPLETED 선정산 누적액) 초과 입력 불가
     * 펀딩 시작일 이전 날짜 입력 불가
     */
    @Transactional
    public FundUsageResponse addFundUsage(Long ideaId, FundUsageRequest request, Long userId) {
        ideaService.validateNotSuspended(ideaId); // 분쟁 처리 중 일시 중단된 프로젝트는 자금 사용 내역 입력 불가
        IdeaResponse idea = ideaService.getIdea(ideaId);
        if (!idea.userId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        milestoneRepository.findByIdeaIdAndStatusWithPessimisticLock(ideaId, MilestoneStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.FUND_USAGE_NO_IN_PROGRESS_MILESTONE));

        // 지출 일자 하한선 검증 — 펀딩 시작일 이전 날짜 불가
        if (request.usedAt().isBefore(idea.fundingStartAt().toLocalDate())) {
            throw new CustomException(ErrorCode.FUND_USAGE_INVALID_DATE);
        }

        // 수령액 초과 지출 방지 — 실제 지급 완료된 선정산 누적액 초과 불가
        long totalReceived = preSettlementRepository.sumAmountByIdeaIdAndStatus(
                ideaId, PreSettlementStatus.COMPLETED);
        long totalUsed = fundUsageRepository.sumAmountByIdeaId(ideaId);

        if (totalUsed + request.amount() > totalReceived) {
            throw new CustomException(ErrorCode.FUND_USAGE_EXCEEDS_RECEIVED);
        }

        FundUsage fundUsage = FundUsage.builder()
                .ideaId(ideaId)
                .itemName(request.itemName())
                .amount(request.amount())
                .usedAt(request.usedAt())
                .build();

        return FundUsageResponse.from(fundUsageRepository.save(fundUsage));
    }

    /**
     * 자금 사용 내역 조회
     * 관리자는 모두 조회 가능
     * 제안자는 본인 프로젝트만 조회 가능
     * 결제 성공 후원자는 해당 프로젝트만 조회 가능
     */
    @Transactional(readOnly = true)
    public List<FundUsageResponse> getFundUsages(Long ideaId, Long userId, Role role) {
        if (role == Role.ADMIN) {
            return toFundUsageResponses(ideaId);
        }

        IdeaResponse idea = ideaService.getIdea(ideaId);
        if (idea.userId().equals(userId)) {
            return toFundUsageResponses(ideaId);
        }

        if (fundingRepository.existsPaidSponsorByIdeaIdAndSponsorId(ideaId, userId)) {
            return toFundUsageResponses(ideaId);
        }

        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    private List<FundUsageResponse> toFundUsageResponses(Long ideaId) {
        return fundUsageRepository.findByIdeaIdOrderByUsedAtDesc(ideaId).stream()
                .map(FundUsageResponse::from)
                .toList();
    }
}