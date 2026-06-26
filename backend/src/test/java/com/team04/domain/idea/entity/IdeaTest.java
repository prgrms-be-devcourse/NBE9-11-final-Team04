package com.team04.domain.idea.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Idea 엔티티의 수정/후원금 누적 규칙을 검증하는 테스트입니다. */
class IdeaTest {

    private Idea idea() {
        return new Idea(
                1L,
                "아이디어",
                IdeaCategory.TECH,
                "한 줄 소개",
                "문제 정의",
                "해결책",
                "목표",
                "고객",
                "경쟁사",
                "팀 소개",
                100000L,
                0L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusMonths(1),
                RewardType.REWARD_POINT,
                null,
                null
        );
    }

    @Test
    @DisplayName("아이디어 수정 시 리워드 유형도 함께 변경된다")
    void update_rewardTypeChanged() {
        Idea idea = idea();

        idea.update(
                "수정 제목",
                IdeaCategory.HEALTH,
                "수정 소개",
                "수정 문제",
                "수정 해결",
                "수정 목표",
                "수정 고객",
                "수정 경쟁사",
                "수정 팀",
                200000L,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusMonths(2),
                RewardType.PAYBACK,
                "https://example.com/image.png"
        );

        assertThat(idea.getRewardType()).isEqualTo(RewardType.PAYBACK);
    }

    @Test
    @DisplayName("환불 처리 시 누적 후원금과 후원자 수는 음수가 되지 않는다")
    void subtractFundingAmount_notNegative() {
        Idea idea = idea();
        idea.addFundingAmount(10000L);

        idea.subtractFundingAmount(20000L);
        idea.subtractFundingAmount(1000L);

        assertThat(idea.getCurrentAmount()).isZero();
        assertThat(idea.getSponsorCount()).isZero();
    }

    @Test
    @DisplayName("작성자 검증 실패 시 공통 FORBIDDEN 예외가 발생한다")
    void validateOwner_forbidden() {
        Idea idea = idea();

        assertThatThrownBy(() -> idea.validateOwner(2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}