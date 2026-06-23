package com.team04.domain.notification.service;

import com.team04.domain.notification.entity.*;
import com.team04.domain.notification.repository.NotificationOutboxRepository;
import com.team04.domain.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxProcessorTest {

    @Mock private NotificationOutboxRepository outboxRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private NotificationOutboxProcessor processor;

    // ─────────────────────────────────────────────
    // processOne - 유저 알림
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("유저 알림 처리 성공 시 SENT로 전환")
    void processOne_유저알림_성공() {
        NotificationOutbox outbox = userOutbox(NotificationPriority.NORMAL);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));

        processor.processOne(1L);

        verify(notificationService).createNotification(
                eq(1L), any(), any(), any(), any());
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
    }

    @Test
    @DisplayName("역할 기반 알림 처리 성공 시 SENT로 전환")
    void processOne_역할알림_성공() {
        NotificationOutbox outbox = roleOutbox(NotificationPriority.NORMAL);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));

        processor.processOne(1L);

        verify(notificationService).createAnnouncementToRole(
                eq(Role.ADMIN), any(), any(), any(), any());
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
    }

    // ─────────────────────────────────────────────
    // processOne - NORMAL 실패
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("NORMAL 알림 처리 실패 시 retryCount 증가")
    void processOne_NORMAL_실패_retryCount증가() {
        NotificationOutbox outbox = userOutbox(NotificationPriority.NORMAL);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        doThrow(new RuntimeException("DB 오류")).when(notificationService)
                .createNotification(any(), any(), any(), any(), any());

        processor.processOne(1L);

        assertThat(outbox.getRetryCount()).isEqualTo(1);
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("NORMAL 알림 3회 실패 시 FAILED로 전환")
    void processOne_NORMAL_3회실패_FAILED() {
        NotificationOutbox outbox = userOutbox(NotificationPriority.NORMAL);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        doThrow(new RuntimeException("DB 오류")).when(notificationService)
                .createNotification(any(), any(), any(), any(), any());

        processor.processOne(1L);
        processor.processOne(1L);
        processor.processOne(1L);

        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(3);
    }

    // ─────────────────────────────────────────────
    // processOne - CRITICAL 실패
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("CRITICAL 알림 실패 시 FAILED로 전환되지 않고 nextRetryAt 설정")
    void processOne_CRITICAL_실패_PENDING유지() {
        NotificationOutbox outbox = userOutbox(NotificationPriority.CRITICAL);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));
        doThrow(new RuntimeException("DB 오류")).when(notificationService)
                .createNotification(any(), any(), any(), any(), any());

        processor.processOne(1L);
        processor.processOne(1L);
        processor.processOne(1L);
        processor.processOne(1L);
        processor.processOne(1L);

        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
        assertThat(outbox.getNextRetryAt()).isNotNull();
        assertThat(outbox.getRetryCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("CRITICAL 알림 재시도 성공 시 SENT로 전환")
    void processOne_CRITICAL_재시도후성공_SENT() {
        NotificationOutbox outbox = userOutbox(NotificationPriority.CRITICAL);
        given(outboxRepository.findById(1L)).willReturn(Optional.of(outbox));

        // 2회 실패
        doThrow(new RuntimeException("일시 오류"))
                .doNothing()
                .when(notificationService)
                .createNotification(any(), any(), any(), any(), any());

        processor.processOne(1L);
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);

        processor.processOne(1L);
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
    }

    // ─────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────

    private NotificationOutbox userOutbox(NotificationPriority priority) {
        NotificationOutbox outbox = NotificationOutbox.forUser(
                1L, NotificationType.DISPUTE_RESOLVED, "제목", "메시지", 10L, priority);
        ReflectionTestUtils.setField(outbox, "id", 1L);
        return outbox;
    }

    private NotificationOutbox roleOutbox(NotificationPriority priority) {
        NotificationOutbox outbox = NotificationOutbox.forRole(
                Role.ADMIN, NotificationType.REPORT_RECEIVED, "제목", "메시지", 10L, priority);
        ReflectionTestUtils.setField(outbox, "id", 1L);
        return outbox;
    }
}
