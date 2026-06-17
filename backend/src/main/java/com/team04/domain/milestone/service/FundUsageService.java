package com.team04.domain.milestone.service;

import com.team04.domain.milestone.dto.request.FundUsageRequest;
import com.team04.domain.milestone.dto.response.FundUsageResponse;
import com.team04.domain.milestone.entity.FundUsage;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.FundUsageRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
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

    /**
     * 자금 사용 내역 입력 (Append Only)
     * 제안자만 가능, 진행 중인 마일스톤이 있어야 함
     */
    @Transactional
    public FundUsageResponse addFundUsage(Long ideaId, FundUsageRequest request) {
        if (request.amount() == null || request.amount() <= 0) {
            throw new CustomException(ErrorCode.FUND_USAGE_INVALID_AMOUNT);
        }

        milestoneRepository.findByIdeaIdAndStatus(ideaId, MilestoneStatus.IN_PROGRESS)
                .orElseThrow(() -> new CustomException(ErrorCode.FUND_USAGE_NO_IN_PROGRESS_MILESTONE));

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
     * 제안자, 후원자, 관리자 모두 가능
     * TODO: 후원자는 해당 프로젝트에 후원한 후원자만 조회 가능하도록 변경 필요
     *       (FundingRepository.existsByIdeaIdAndSponsorId 추가 요청 중)
     */
    @Transactional(readOnly = true)
    public List<FundUsageResponse> getFundUsages(Long ideaId) {
        return fundUsageRepository.findByIdeaIdOrderByUsedAtDesc(ideaId).stream()
                .map(FundUsageResponse::from)
                .toList();
    }
}