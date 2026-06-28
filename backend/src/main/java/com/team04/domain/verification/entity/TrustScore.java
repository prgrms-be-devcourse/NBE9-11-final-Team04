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
    private Integer totalScore = 0;

    /** 아이디어 식별자로 기본 점수 0점의 신뢰도 점수를 생성합니다. */
    public TrustScore(Long ideaId) {
        this.ideaId = ideaId;
    }

    /** 항목별 점수를 상한 20점으로 보정하고 총점을 갱신합니다. */
    public void updateScores(
            Integer aiVerificationScore,
            Integer milestoneSpecificityScore,
            Integer expertMatchingScore,
            Integer adminApprovalScore,
            Integer proposerHistoryScore
    ) {
        this.aiVerificationScore = limitItemScore(aiVerificationScore);
        this.milestoneSpecificityScore = limitItemScore(milestoneSpecificityScore);
        this.expertMatchingScore = limitItemScore(expertMatchingScore);
        this.adminApprovalScore = limitItemScore(adminApprovalScore);
        this.proposerHistoryScore = limitItemScore(proposerHistoryScore);
        this.totalScore = calculateTotalScore();
    }

    /** 개별 점수를 0점 이상 20점 이하 범위로 보정합니다. */
    private int limitItemScore(Integer score) {
        if (score == null) {
            return 0;
        }
        return Math.max(0, Math.min(score, ITEM_MAX_SCORE));
    }

    /** 항목별 점수를 합산하고 총점 상한 100점을 적용합니다. */
    private int calculateTotalScore() {
        int total = aiVerificationScore
                + milestoneSpecificityScore
                + expertMatchingScore
                + adminApprovalScore
                + proposerHistoryScore;
        return Math.min(total, ITEM_MAX_SCORE * 5);
    }

    /** 관리자 승인 점수를 갱신하고 총점을 재계산합니다. */
    public void updateAdminApprovalScore(Integer score) {
        this.adminApprovalScore = limitItemScore(score);
        this.totalScore = calculateTotalScore();
    }
}