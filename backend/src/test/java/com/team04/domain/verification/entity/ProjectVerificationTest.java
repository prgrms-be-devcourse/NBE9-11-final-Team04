package com.team04.domain.verification.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** ProjectVerification 상태 전이를 검증하는 테스트입니다. */
class ProjectVerificationTest {

    /** 검증 시작 시 AI_VERIFYING 상태로 전이되는지 확인합니다. */
    @Test
    @DisplayName("검증 시작 시 DRAFT에서 AI_VERIFYING으로 전이 성공")
    void startAiVerification_성공() {
        ProjectVerification verification = new ProjectVerification(1L);

        verification.startAiVerification();

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.AI_VERIFYING);
    }

    /** 참고용 검증 결과 저장 완료 시 AI_PASSED 상태로 전이되는지 확인합니다. */
    @Test
    @DisplayName("참고용 검증 완료 시 AI_PASSED로 전이 성공")
    void completeAiVerification_성공() {
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.completeAiVerification();

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.AI_PASSED);
    }

    /** OpenAI 장애 발생 시 관리자 재시도 상태로 전이되는지 확인합니다. */
    @Test
    @DisplayName("AI 검증 중 장애 상태 전이 성공")
    void markPendingAdminReview_성공() {
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();

        verification.markPendingAdminReview();

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.PENDING_ADMIN_REVIEW);
    }

    /** DRAFT에서 검증 완료 상태로 직접 전이할 수 없는지 확인합니다. */
    @Test
    @DisplayName("잘못된 상태 전이 예외 발생")
    void invalidTransition_예외() {
        ProjectVerification verification = new ProjectVerification(1L);

        assertThatThrownBy(verification::completeAiVerification)
                .isInstanceOf(RuntimeException.class);
    }
}

