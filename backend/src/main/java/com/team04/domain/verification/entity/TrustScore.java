package com.team04.domain.verification.entity;

import com.team04.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 프로젝트 검증의 항목별 신뢰도 점수와 합산 점수를 저장하는 엔티티입니다. */
@Entity
@Table(name = "trust_score")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrustScore extends BaseEntity {

    private static final int ITEM_MAX_SCORE = 20;
    private static final int BUSINESS_REGISTRATION_BONUS_SCORE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ideaId;

    @Column(nullable = false)
    private Integer aiVerificationScore = 0;

    @Column(nullable = false)
    private Integer milestoneSpecificityScore = 0;

    @Column(nullable = false)
    private Integer expertMatchingScore = 0;

    @Column(nullable = false)
    private Integer adminApprovalScore = 0;

    @Column(nullable = false)
    private Integer proposerHistoryScore = 0;

    @Column(nullable = false)
    private Integer businessRegistrationBonus = 0;

    @Column(nullable = false)
    private Integer totalScore = 0;

    /** 아이디어 식별자로 기본 점수 0점의 신뢰도 점수를 생성합니다. */
    public TrustScore(Long ideaId) {
        this.ideaId = ideaId;
    }

    /** 항목별 점수를 상한 20점으로 보정하고 사업자등록 가점을 반영해 총점을 갱신합니다. */
    public void updateScores(
            Integer aiVerificationScore,
            Integer milestoneSpecificityScore,
            Integer expertMatchingScore,
            Integer adminApprovalScore,
            Integer proposerHistoryScore,
            boolean hasBusinessRegistration
    ) {
        this.aiVerificationScore = limitItemScore(aiVerificationScore);
        this.milestoneSpecificityScore = limitItemScore(milestoneSpecificityScore);
        this.expertMatchingScore = limitItemScore(expertMatchingScore);
        this.adminApprovalScore = limitItemScore(adminApprovalScore);
        this.proposerHistoryScore = limitItemScore(proposerHistoryScore);
        this.businessRegistrationBonus = hasBusinessRegistration ? BUSINESS_REGISTRATION_BONUS_SCORE : 0;
        this.totalScore = calculateTotalScore();
    }

    /** 개별 점수를 0점 이상 20점 이하 범위로 보정합니다. */
    private int limitItemScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(score, ITEM_MAX_SCORE));
    }

    /** 항목별 점수와 사업자등록 가점을 합산한 총점을 계산합니다. */
    private int calculateTotalScore() {
        return aiVerificationScore
                + milestoneSpecificityScore
                + expertMatchingScore
                + adminApprovalScore
                + proposerHistoryScore
                + businessRegistrationBonus;
    }
}