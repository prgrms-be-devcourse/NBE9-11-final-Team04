package com.team04.domain.notification.entity;

import com.team04.domain.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import java.time.temporal.ChronoUnit;

class NotificationOutboxTest {

    // ─────────────────────────────────────────────
    // factory
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("forUser - 유저 알림 생성 시 PENDING 상태로 초기화")
    void forUser_초기상태() {
        NotificationOutbox outbox = NotificationOutbox.forUser(
                1L, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 10L,
                NotificationPriority.CRITICAL);

        assertThat(outbox.getUserId()).isEqualTo(1L);
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
        assertThat(outbox.getPriority()).isEqualTo(NotificationPriority.CRITICAL);
        assertThat(outbox.getRetryCount()).isZero();
        assertThat(outbox.getNextRetryAt()).isNull();
        assertThat(outbox.isRoleBased()).isFalse();
    }

    @Test
    @DisplayName("forRole - 역할 기반 알림 생성 시 isRoleBased true")
    void forRole_초기상태() {
        NotificationOutbox outbox = NotificationOutbox.forRole(
                Role.ADMIN, NotificationType.REPORT_RECEIVED, "제목", "메시지", 10L,
                NotificationPriority.NORMAL);

        assertThat(outbox.getTargetRole()).isEqualTo(Role.ADMIN);
        assertThat(outbox.isRoleBased()).isTrue();
        assertThat(outbox.getUserId()).isNull();
    }

    // ─────────────────────────────────────────────
    // markSent
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("markSent - status SENT, processedAt 설정")
    void markSent_성공() {
        NotificationOutbox outbox = normalOutbox();

        outbox.markSent();

        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
        assertThat(outbox.getProcessedAt()).isNotNull();
        assertThat(outbox.getProcessedAt()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
    }

    // ─────────────────────────────────────────────
    // NORMAL - incrementRetryOrFail
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("NORMAL - 3회 미만 실패는 PENDING 유지")
    void normal_3회미만_PENDING유지() {
        NotificationOutbox outbox = normalOutbox();

        outbox.incrementRetryOrFail();
        outbox.incrementRetryOrFail();

        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("NORMAL - 3회 실패 시 FAILED로 전환")
    void normal_3회실패_FAILED() {
        NotificationOutbox outbox = normalOutbox();

        outbox.incrementRetryOrFail();
        outbox.incrementRetryOrFail();
        outbox.incrementRetryOrFail();

        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(3);
        assertThat(outbox.getProcessedAt()).isNotNull();
    }

    // ─────────────────────────────────────────────
    // CRITICAL - incrementRetryOrFail
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("CRITICAL - 실패해도 FAILED로 전환되지 않음")
    void critical_실패해도_FAILED없음() {
        NotificationOutbox outbox = criticalOutbox();

        for (int i = 0; i < 10; i++) {
            outbox.incrementRetryOrFail();
        }

        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("CRITICAL - 1회 실패 시 nextRetryAt +30초")
    void critical_1회실패_backoff_30초() {
        NotificationOutbox outbox = criticalOutbox();
        LocalDateTime before = LocalDateTime.now();

        outbox.incrementRetryOrFail();

        assertThat(outbox.getNextRetryAt()).isAfter(before.plusSeconds(29));
        assertThat(outbox.getNextRetryAt()).isBefore(before.plusSeconds(31));
    }

    @Test
    @DisplayName("CRITICAL - 2회 실패 시 nextRetryAt +5분")
    void critical_2회실패_backoff_5분() {
        NotificationOutbox outbox = criticalOutbox();
        LocalDateTime before = LocalDateTime.now();

        outbox.incrementRetryOrFail();
        outbox.incrementRetryOrFail();

        assertThat(outbox.getNextRetryAt()).isAfter(before.plusSeconds(299));
        assertThat(outbox.getNextRetryAt()).isBefore(before.plusSeconds(301));
    }

    @Test
    @DisplayName("CRITICAL - 3회 실패 시 nextRetryAt +30분")
    void critical_3회실패_backoff_30분() {
        NotificationOutbox outbox = criticalOutbox();
        LocalDateTime before = LocalDateTime.now();

        outbox.incrementRetryOrFail();
        outbox.incrementRetryOrFail();
        outbox.incrementRetryOrFail();

        assertThat(outbox.getNextRetryAt()).isAfter(before.plusSeconds(1799));
        assertThat(outbox.getNextRetryAt()).isBefore(before.plusSeconds(1801));
    }

    @Test
    @DisplayName("CRITICAL - 4회 이상 실패 시 nextRetryAt +1시간으로 고정")
    void critical_4회이상실패_backoff_1시간_고정() {
        NotificationOutbox outbox = criticalOutbox();
        LocalDateTime before = LocalDateTime.now();

        for (int i = 0; i < 5; i++) {
            outbox.incrementRetryOrFail();
        }

        assertThat(outbox.getNextRetryAt()).isAfter(before.plusSeconds(3599));
        assertThat(outbox.getNextRetryAt()).isBefore(before.plusSeconds(3601));
    }

    // ─────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────

    private NotificationOutbox normalOutbox() {
        return NotificationOutbox.forUser(
                1L, NotificationType.MATCH_ACCEPTED, "제목", "메시지", 1L,
                NotificationPriority.NORMAL);
    }

    private NotificationOutbox criticalOutbox() {
        return NotificationOutbox.forUser(
                1L, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 1L,
                NotificationPriority.CRITICAL);
    }
}
