package com.team04.domain.milestone.service;

import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.milestone.dto.request.FundUsageRequest;
import com.team04.domain.milestone.dto.response.FundUsageResponse;
import com.team04.domain.milestone.entity.FundUsage;
import com.team04.domain.milestone.entity.MilestoneStatus;
import com.team04.domain.milestone.repository.FundUsageRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
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
    private final IdeaService ideaService;

    /**
     * 자금 사용 내역 입력 (Append Only)
     * 제안자만 가능, 본인 프로젝트인지 소유권 검증
     * 진행 중인 마일스톤이 있어야 함
     */
    @Transactional
    public FundUsageResponse addFundUsage(Long ideaId, FundUsageRequest request, Long userId) {
        IdeaResponse idea = ideaService.getIdea(ideaId);
        if (!idea.userId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
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
     * 관리자는 모두 조회 가능
     * 제안자는 본인 프로젝트만 조회 가능
     * 후원자는 모두 조회 가능 (TODO: 해당 프로젝트에 후원한 후원자만 조회 가능하도록 변경 필요)
     */
    @Transactional(readOnly = true)
    public List<FundUsageResponse> getFundUsages(Long ideaId, Long userId, Role role) {
        if (role == Role.PROPOSER) {
            IdeaResponse idea = ideaService.getIdea(ideaId);
            if (!idea.userId().equals(userId)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }

        return fundUsageRepository.findByIdeaIdOrderByUsedAtDesc(ideaId).stream()
                .map(FundUsageResponse::from)
                .toList();
    }
}