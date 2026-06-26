package com.team04.domain.dispute.entity;

import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisputeStatusTest {

    // ─────────────────────────────────────────────
    // canTransitionTo
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("RECEIVED → PENDING 허용")
    void RECEIVED_to_PENDING_허용() {
        assertThat(DisputeStatus.RECEIVED.canTransitionTo(DisputeStatus.PENDING)).isTrue();
    }

    @Test
    @DisplayName("RECEIVED → RESOLVED 불허")
    void RECEIVED_to_RESOLVED_불허() {
        assertThat(DisputeStatus.RECEIVED.canTransitionTo(DisputeStatus.RESOLVED)).isFalse();
    }

    @Test
    @DisplayName("RECEIVED → REJECTED 불허")
    void RECEIVED_to_REJECTED_불허() {
        assertThat(DisputeStatus.RECEIVED.canTransitionTo(DisputeStatus.REJECTED)).isFalse();
    }

    @Test
    @DisplayName("PENDING → RESOLVED 허용")
    void PENDING_to_RESOLVED_허용() {
        assertThat(DisputeStatus.PENDING.canTransitionTo(DisputeStatus.RESOLVED)).isTrue();
    }

    @Test
    @DisplayName("PENDING → REJECTED 허용")
    void PENDING_to_REJECTED_허용() {
        assertThat(DisputeStatus.PENDING.canTransitionTo(DisputeStatus.REJECTED)).isTrue();
    }

    @Test
    @DisplayName("PENDING → RECEIVED 허용 (관리자 소명 재요청)")
    void PENDING_to_RECEIVED_허용() {
        assertThat(DisputeStatus.PENDING.canTransitionTo(DisputeStatus.RECEIVED)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(DisputeStatus.class)
    @DisplayName("RESOLVED는 어떤 상태로도 전이 불허")
    void RESOLVED_전이_전부_불허(DisputeStatus next) {
        assertThat(DisputeStatus.RESOLVED.canTransitionTo(next)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(DisputeStatus.class)
    @DisplayName("REJECTED는 어떤 상태로도 전이 불허")
    void REJECTED_전이_전부_불허(DisputeStatus next) {
        assertThat(DisputeStatus.REJECTED.canTransitionTo(next)).isFalse();
    }

    // ─────────────────────────────────────────────
    // Dispute.updateStatus
    // ─────────────────────────────────────────────

    private Dispute createDispute() {
        User reporter = User.create("a@test.com", "pw", "신고자", "reporter", 30, Role.USER);
        User reported = User.create("b@test.com", "pw", "피신고자", "reported", 30, Role.USER);
        Dispute dispute = new Dispute(reporter, reported, TargetType.IDEA, 1L,
                DisputeCategory.IDEA_THEFT, "제목", "이유", null);
        ReflectionTestUtils.setField(dispute, "createdAt", LocalDateTime.now());
        return dispute;
    }

    @Test
    @DisplayName("유효한 전이 RECEIVED → PENDING 성공")
    void updateStatus_유효한전이_성공() {
        Dispute dispute = createDispute();
        dispute.updateStatus(DisputeStatus.PENDING);
        assertThat(dispute.getStatus()).isEqualTo(DisputeStatus.PENDING);
    }

    @Test
    @DisplayName("유효하지 않은 전이 RECEIVED → RESOLVED는 예외 발생")
    void updateStatus_유효하지않은전이_예외() {
        Dispute dispute = createDispute();
        assertThatThrownBy(() -> dispute.updateStatus(DisputeStatus.RESOLVED))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPUTE_INVALID_STATUS_TRANSITION);
    }

    // ─────────────────────────────────────────────
    // Dispute.isAppealable
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("RECEIVED 상태에서 소명 가능")
    void isAppealable_RECEIVED_true() {
        Dispute dispute = createDispute();
        assertThat(dispute.isAppealable()).isTrue();
    }

    @Test
    @DisplayName("PENDING 상태에서 소명 불가")
    void isAppealable_PENDING_false() {
        Dispute dispute = createDispute();
        dispute.updateStatus(DisputeStatus.PENDING);
        assertThat(dispute.isAppealable()).isFalse();
    }

    @Test
    @DisplayName("RESOLVED 상태에서 소명 불가")
    void isAppealable_RESOLVED_false() {
        Dispute dispute = createDispute();
        dispute.updateStatus(DisputeStatus.PENDING);
        dispute.updateStatus(DisputeStatus.RESOLVED);
        assertThat(dispute.isAppealable()).isFalse();
    }

    @Test
    @DisplayName("REJECTED 상태에서 소명 불가")
    void isAppealable_REJECTED_false() {
        Dispute dispute = createDispute();
        dispute.updateStatus(DisputeStatus.PENDING);
        dispute.updateStatus(DisputeStatus.REJECTED);
        assertThat(dispute.isAppealable()).isFalse();
    }
}
