package com.team04.domain.milestone.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MilestoneTest {

    @Test
    @DisplayName("마일스톤은 PENDING -> IN_PROGRESS -> COMPLETED 순서로 전이된다")
    void milestoneStatusTransition_success() {
        Milestone milestone = createMilestone(1);

        milestone.start();
        milestone.complete();

        assertThat(milestone.getStatus()).isEqualTo(MilestoneStatus.COMPLETED);
    }

    @Test
    @DisplayName("완료된 마일스톤은 다시 취소할 수 없다")
    void cancel_completedMilestone_throwsException() {
        Milestone milestone = createMilestone(1);
        milestone.start();
        milestone.complete();

        assertThatThrownBy(milestone::cancel)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_MILESTONE_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("마일스톤 단계는 1단계부터 3단계까지만 생성할 수 있다")
    void create_invalidStep_throwsException() {
        assertThatThrownBy(() -> createMilestone(4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("마일스톤 단계는 1~3 사이여야 합니다");
    }

    @Test
    @DisplayName("기한 초과 시각을 기록하고 초기화할 수 있다")
    void overdueAt_markAndClear_success() {
        Milestone milestone = createMilestone(1);
        LocalDateTime overdueAt = LocalDateTime.now();

        milestone.markOverdue(overdueAt);
        milestone.clearOverdue();

        assertThat(milestone.getOverdueAt()).isNull();
    }

    private static Milestone createMilestone(int step) {
        return Milestone.builder()
                .ideaId(1L)
                .step(step)
                .goal(step + "단계 목표")
                .expectedResult(step + "단계 결과물")
                .expectedDate(LocalDate.now().plusDays(7))
                .build();
    }
}
