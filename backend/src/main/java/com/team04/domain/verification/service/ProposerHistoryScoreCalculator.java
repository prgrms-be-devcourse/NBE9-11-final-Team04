package com.team04.domain.verification.service;

import com.team04.domain.businessregistration.repository.BusinessRegistrationRepository;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.repository.DisputeRepository;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 제안자 이력 점수(0~20점)를 계산합니다.
 * <ul>
 *   <li>기본 10점 (신규 제안자 불이익 방지)</li>
 *   <li>사업자 등록 인증 시 +5점</li>
 *   <li>이전 프로젝트 성공(COMPLETED) 1건당 +5점</li>
 *   <li>RESOLVED 분쟁(피신고자 기준) 1건당 -5점</li>
 *   <li>최솟값 0점, 최댓값 20점으로 보정</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProposerHistoryScoreCalculator {

    private static final int BASE_SCORE = 10;
    private static final int BUSINESS_REGISTRATION_BONUS = 5;
    private static final int SUCCESS_BONUS_PER_PROJECT = 5;
    private static final int DISPUTE_PENALTY_PER_CASE = 5;
    private static final int MAX_SCORE = 20;
    private static final int MIN_SCORE = 0;

    private final IdeaRepository ideaRepository;
    private final DisputeRepository disputeRepository;
    private final BusinessRegistrationRepository businessRegistrationRepository;

    public int calculate(Long proposerUserId) {
        if (proposerUserId == null) {
            return BASE_SCORE;
        }
        int score = BASE_SCORE;

        if (businessRegistrationRepository.existsByUserId(proposerUserId)) {
            score += BUSINESS_REGISTRATION_BONUS;
        }

        long successCount = ideaRepository.countByUserIdAndStatusAndDeletedAtIsNull(
                proposerUserId, IdeaStatus.COMPLETED);
        score += (int) successCount * SUCCESS_BONUS_PER_PROJECT;

        long resolvedDisputeCount = disputeRepository.countByReportedIdAndStatus(
                proposerUserId, DisputeStatus.RESOLVED);
        score -= (int) resolvedDisputeCount * DISPUTE_PENALTY_PER_CASE;

        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }
}
