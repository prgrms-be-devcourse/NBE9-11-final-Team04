package com.team04.domain.idea.service;

import com.team04.domain.funding.dto.response.DepositResponse;
import com.team04.domain.funding.repository.DepositRepository;
import com.team04.domain.idea.dto.response.AdminIdeaReviewResponse;
import com.team04.domain.idea.dto.response.AdminIdeaReviewSummary;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.match.service.ExpertReviewService;
import com.team04.domain.milestone.dto.response.MilestoneResponse;
import com.team04.domain.milestone.service.MilestoneService;
import com.team04.domain.verification.dto.response.VerificationResponse;
import com.team04.domain.verification.repository.TrustScoreRepository;
import com.team04.domain.verification.service.VerificationService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminIdeaReviewService {

    private final IdeaAdminService ideaAdminService;
    private final IdeaRepository ideaRepository;
    private final VerificationService verificationService;
    private final ExpertReviewService expertReviewService;
    private final TrustScoreRepository trustScoreRepository;
    private final IdeaService ideaService;

    // 검토 대기 목록 조회
    @Transactional(readOnly = true)
    public Page<AdminIdeaReviewResponse> getReviews(IdeaStatus status, Pageable pageable) {
        return ideaAdminService.getReviews(status, pageable);
    }

    @Transactional(readOnly = true)
    public AdminIdeaReviewSummary getReviewSummary(Long ideaId, CustomUserDetails userDetails) {
        // 아이디어 상세 조회
        IdeaResponse idea = ideaService.getIdea(ideaId, userDetails.getUserId(), userDetails.getRole());

        // AI 검증 결과 조회
        VerificationResponse verification = null;
        try {
            verification = verificationService
                    .getVerificationByIdeaId(ideaId, userDetails.getUserId(), userDetails.getRole());
        } catch (CustomException e) {
            // 검증 데이터 없을 경우 null 허용
        }

        // 전문가 검토 의견 조회
        List<ExpertReviewResponse> expertReviews = expertReviewService.getReviewsByIdeaId(ideaId);

        return new AdminIdeaReviewSummary(idea, verification, expertReviews);
    }

    // 승인 + TrustScore adminApprovalScore 업데이트
    @Transactional
    public void approve(Long ideaId) {
        var idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        int score = idea.calculateAdminApprovalScore();

        ideaAdminService.approve(ideaId);

        // TrustScore 업데이트
        trustScoreRepository.findByIdeaId(ideaId).ifPresent(trustScore -> {
            trustScore.updateScores(
                    trustScore.getAiVerificationScore(),
                    trustScore.getMilestoneSpecificityScore(),
                    trustScore.getExpertMatchingScore(),
                    score,  // adminApprovalScore
                    trustScore.getProposerHistoryScore()
            );
            trustScoreRepository.save(trustScore);
        });
    }

    // 거절
    @Transactional
    public void reject(Long ideaId, String reason) {
        ideaAdminService.reject(ideaId, reason);
    }
}