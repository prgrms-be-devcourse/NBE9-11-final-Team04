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
                30000L,
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

    @Test
    @DisplayName("유효한 상태 전이는 성공하고 상태가 변경된다")
    void changeStatus_유효한전이_상태변경성공() {
        Idea idea = idea(); // 초기 상태: AI_PENDING

        idea.changeStatus(IdeaStatus.EXPERT_PENDING);

        assertThat(idea.getStatus()).isEqualTo(IdeaStatus.EXPERT_PENDING);
    }

    @Test
    @DisplayName("유효하지 않은 상태 전이 시 INVALID_IDEA_STATUS_TRANSITION 예외가 발생한다")
    void changeStatus_유효하지않은전이_INVALID_IDEA_STATUS_TRANSITION_예외() {
        Idea idea = idea(); // 초기 상태: AI_PENDING

        assertThatThrownBy(() -> idea.changeStatus(IdeaStatus.OPEN))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("OPEN 상태에서 validateEditable 호출 시 IDEA_STATUS_NOT_EDITABLE 예외가 발생한다")
    void validateEditable_OPEN상태_IDEA_STATUS_NOT_EDITABLE_예외() {
        Idea idea = idea();
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        idea.changeStatus(IdeaStatus.OPEN);

        assertThatThrownBy(idea::validateEditable)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDEA_STATUS_NOT_EDITABLE);
    }

    @Test
    @DisplayName("REJECTED 상태에서 validateEditable 호출 시 예외 없이 통과한다")
    void validateEditable_REJECTED상태_성공() {
        Idea idea = idea();
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        idea.reject("보완 필요");

        idea.validateEditable(); // 예외 없이 통과해야 함
    }

    @Test
    @DisplayName("reject 호출 시 상태가 REJECTED로 전이되고 rejectReason이 저장된다")
    void reject_상태전이및반려정보저장() {
        Idea idea = idea();
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);

        idea.reject("보완 필요");

        assertThat(idea.getStatus()).isEqualTo(IdeaStatus.REJECTED);
        assertThat(idea.getRejectReason()).isEqualTo("보완 필요");
    }

    @Test
    @DisplayName("suspend 후 restore 호출 시 이전 상태로 복원된다")
    void restore_이전상태로복원() {
        Idea idea = idea();
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        idea.changeStatus(IdeaStatus.OPEN);
        idea.suspend();

        idea.restore();

        assertThat(idea.getStatus()).isEqualTo(IdeaStatus.OPEN);
    }

    @Test
    @DisplayName("SUSPENDED가 아닌 상태에서 restore 호출 시 INVALID_IDEA_STATUS_TRANSITION 예외가 발생한다")
    void restore_SUSPENDED아닌상태_INVALID_IDEA_STATUS_TRANSITION_예외() {
        Idea idea = idea(); // 초기 상태: AI_PENDING

        assertThatThrownBy(idea::restore)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
    }
}