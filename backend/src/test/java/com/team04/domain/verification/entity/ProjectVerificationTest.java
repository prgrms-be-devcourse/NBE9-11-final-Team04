package com.team04.domain.verification.entity;

import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectVerificationTest {

    @Test
    @DisplayName("검증 시작 시 DRAFT에서 AI_VERIFYING으로 전이 성공")
    void startAiVerification_성공() {
        // 검증 시작 로직이 최초 상태에서 AI 검증 상태로 변경되는지 확인한다.
        ProjectVerification verification = new ProjectVerification(1L);

        verification.startAiVerification();

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.AI_VERIFYING);
    }

    @Test
    @DisplayName("보완 요청 시 NEEDS_REVISION 상태와 보완 기한 저장 성공")
    void requestRevision_성공() {
        // AI 검증 중 보완 요청이 발생하면 보완 상태와 마감 시간이 함께 저장되는지 확인한다.
        ProjectVerification verification = new ProjectVerification(1L);
        LocalDateTime revisionDueAt = LocalDateTime.now().plusDays(7);
        verification.startAiVerification();

        verification.requestRevision(revisionDueAt);

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.NEEDS_REVISION);
        assertThat(verification.getRevisionDueAt()).isEqualTo(revisionDueAt);
    }

    @Test
    @DisplayName("재제출 시 횟수 증가와 보완 기한 초기화 성공")
    void resubmit_성공() {
        // 보완 상태에서 재제출하면 AI 검증으로 돌아가고 재제출 횟수와 보완 기한이 갱신되는지 확인한다.
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.requestRevision(LocalDateTime.now().plusDays(7));

        verification.resubmit();

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.AI_VERIFYING);
        assertThat(verification.getResubmissionCount()).isEqualTo(1);
        assertThat(verification.getRevisionDueAt()).isNull();
    }

    @Test
    @DisplayName("30일 대기 반려 시 REJECTED 상태와 대기 종료 시간 저장 성공")
    void rejectWithWaiting_성공() {
        // 재제출 제한 초과 시 반려 상태와 대기 종료 시간이 함께 저장되는지 확인한다.
        ProjectVerification verification = new ProjectVerification(1L);
        LocalDateTime waitingUntil = LocalDateTime.now().plusDays(30);
        verification.startAiVerification();
        verification.requestRevision(LocalDateTime.now().plusDays(7));

        verification.rejectWithWaiting(waitingUntil);

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(verification.getWaitingUntil()).isEqualTo(waitingUntil);
    }

    @Test
    @DisplayName("유효하지 않은 상태 전이 시 예외 발생")
    void changeStatus_유효하지않은전이() {
        // DRAFT 상태에서 바로 반려 상태로 변경할 수 없는지 확인한다.
        ProjectVerification verification = new ProjectVerification(1L);

        assertThatThrownBy(() -> verification.changeStatus(VerificationStatus.REJECTED))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VERIFICATION_STATUS_TRANSITION);
    }
}

