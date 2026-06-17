package com.team04.domain.verification.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrustScoreTest {

    @Test
    @DisplayName("신뢰도 점수 생성 시 기본값은 0점")
    void create_기본점수() {
        // 신뢰도 점수 엔티티가 생성될 때 모든 항목 점수가 0점인지 확인한다.
        TrustScore trustScore = new TrustScore(1L);

        assertThat(trustScore.getIdeaId()).isEqualTo(1L);
        assertThat(trustScore.getAiVerificationScore()).isZero();
        assertThat(trustScore.getMilestoneSpecificityScore()).isZero();
        assertThat(trustScore.getTotalScore()).isZero();
    }

    @Test
    @DisplayName("항목별 점수 갱신 시 20점 상한과 0점 하한 적용")
    void updateScores_범위보정() {
        // 항목별 점수가 허용 범위를 벗어나면 0점 이상 20점 이하로 보정되는지 확인한다.
        TrustScore trustScore = new TrustScore(1L);

        trustScore.updateScores(30, -5, null, 15, 10);

        assertThat(trustScore.getAiVerificationScore()).isEqualTo(20);
        assertThat(trustScore.getMilestoneSpecificityScore()).isZero();
        assertThat(trustScore.getExpertMatchingScore()).isZero();
        assertThat(trustScore.getAdminApprovalScore()).isEqualTo(15);
        assertThat(trustScore.getProposerHistoryScore()).isEqualTo(10);
        assertThat(trustScore.getTotalScore()).isEqualTo(55);
    }

    @Test
    @DisplayName("총점 계산 시 최대 100점 상한 적용")
    void updateScores_총점상한() {
        // 모든 항목이 최고점을 초과해도 총점은 100점을 넘지 않는지 확인한다.
        TrustScore trustScore = new TrustScore(1L);

        trustScore.updateScores(100, 100, 100, 100, 100);

        assertThat(trustScore.getTotalScore()).isEqualTo(100);
    }
}
