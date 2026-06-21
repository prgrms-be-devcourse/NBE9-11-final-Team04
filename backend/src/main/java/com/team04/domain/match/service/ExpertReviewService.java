package com.team04.domain.match.service;

import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.match.dto.response.ExpertReviewResponse;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.match.entity.ExpertReview;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.match.repository.ExpertReviewRepository;
import com.team04.domain.match.entity.ExpertMatch;
import com.team04.domain.match.entity.MatchStatus;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpertReviewService {

    private final ExpertMatchRepository expertMatchRepository;
    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertReviewRepository expertReviewRepository;

    @Transactional
    public ExpertReviewResponse createReview(Long userId, Long matchId, ExpertReviewRequest request) {

        // 전문가 프로필 조회 및 검증 완료 여부 확인
        ExpertProfile expertProfile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        if (!expertProfile.isVerified()) {
            throw new CustomException(ErrorCode.EXPERT_NOT_VERIFIED);
        }

        // 매칭 조회 및 본인 매칭인지 확인
        ExpertMatch match = expertMatchRepository.findByIdAndUserId(matchId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATCH_NOT_FOUND));

        // 수락된 매칭인지 확인
        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new CustomException(ErrorCode.MATCH_NOT_ACCEPTED);
        }

        // 이미 리뷰가 작성된 매칭인지 확인
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
        return ExpertReviewResponse.from(review);
    }
}