package com.team04.domain.match.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.match.entity.ExpertReview;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.match.repository.ExpertReviewRepository;
import com.team04.domain.match.entity.ExpertMatch;
import com.team04.domain.match.entity.MatchStatus;
import com.team04.domain.notification.entity.NotificationPriority;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.verification.entity.TrustScore;
import com.team04.domain.verification.repository.TrustScoreRepository;
import com.team04.global.event.NotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertReviewService {

    private final ExpertMatchRepository expertMatchRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertReviewRepository expertReviewRepository;
    private final IdeaRepository ideaRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ExpertReviewResponse createReview(Long userId, Long matchId, ExpertReviewRequest request) {

        ExpertProfile expertProfile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        if (!expertProfile.isVerified()) {
            throw new CustomException(ErrorCode.EXPERT_NOT_VERIFIED);
        }

        ExpertMatch match = expertMatchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));

        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new CustomException(ErrorCode.MATCH_NOT_ACCEPTED);
        }

        if (expertReviewRepository.existsByExpertMatch_Id(matchId)) {
            throw new CustomException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        ExpertReview review = ExpertReview.create(
                expertProfile,
                match,
                request.feasibility(),
                request.expectedPeriod(),
                request.techStack(),
                request.riskFactor(),
                request.opinion()
        );
        expertReviewRepository.save(review);

        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(match.getIdeaId())
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        // EXPERT_PENDING 상태일 때만 ADMIN_PENDING으로 전환
        if (idea.getStatus() == IdeaStatus.EXPERT_PENDING) {
            idea.changeStatus(IdeaStatus.ADMIN_PENDING);
            ideaRepository.save(idea);
        }

        // 신뢰도 점수 반영 (Feasibility 기반)
        trustScoreRepository.findByIdeaId(idea.getId()).ifPresent(trustScore -> {
            int score = switch (request.feasibility()) {
                case POSSIBLE -> 20;
                case IMPOSSIBLE -> 10;
            };
            trustScore.updateScores(
                    trustScore.getAiVerificationScore(),
                    trustScore.getMilestoneSpecificityScore(),
                    score,
                    trustScore.getAdminApprovalScore(),
                    trustScore.getProposerHistoryScore()
            );
            trustScoreRepository.save(trustScore);
        });

        // 제안자에게 알림 발송
        eventPublisher.publishEvent(new NotificationEvent(
                idea.getUserId(),
                NotificationType.IDEA_EXPERT_APPROVED,
                "전문가 검증 완료",
                "전문가 검증서가 제출되었습니다. 관리자 최종 승인 단계로 이동했습니다.",
                idea.getId(),
                NotificationPriority.NORMAL
        ));

        return ExpertReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<ExpertReviewResponse> getReviewsByIdeaId(Long ideaId) {
        return expertReviewRepository.findByIdeaId(ideaId)
                .stream()
                .map(ExpertReviewResponse::from)
                .toList();
    }
}
