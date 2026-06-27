package com.team04.domain.notification.service;

import com.team04.domain.notification.entity.NotificationOutbox;
import com.team04.domain.notification.entity.NotificationPriority;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.notification.repository.NotificationOutboxRepository;
import com.team04.domain.user.entity.Role;
import com.team04.global.event.NotificationEvent;
import com.team04.global.event.ReportNotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationOutboxRepository outboxRepository;

    @InjectMocks
    private NotificationEventListener eventListener;

    // ─────────────────────────────────────────────
    // handleNotificationEvent
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("유저 알림 이벤트 수신 시 Outbox에 저장")
    void handleNotificationEvent_유저알림_Outbox저장() {
        NotificationEvent event = new NotificationEvent(
                1L,
                NotificationType.DISPUTE_RESOLVED,
                "신고 처리 완료",
                "신고 내용이 처리되었습니다",
                10L
        );

        eventListener.handleNotificationEvent(event);

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        then(outboxRepository).should().save(captor.capture());

        NotificationOutbox saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.DISPUTE_RESOLVED);
        assertThat(saved.getTitle()).isEqualTo("신고 처리 완료");
        assertThat(saved.getPriority()).isEqualTo(NotificationPriority.NORMAL);
    }

    @Test
    @DisplayName("유저 알림 이벤트 - CRITICAL 우선순위 보존")
    void handleNotificationEvent_CRITICAL우선순위_보존() {
        NotificationEvent event = new NotificationEvent(
                1L,
                NotificationType.DISPUTE_RESOLVED,
                "중요 알림",
                "메시지",
                10L,
                NotificationPriority.CRITICAL
        );

        eventListener.handleNotificationEvent(event);

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        then(outboxRepository).should().save(captor.capture());

        assertThat(captor.getValue().getPriority()).isEqualTo(NotificationPriority.CRITICAL);
    }

    // ─────────────────────────────────────────────
    // handleReportEvent
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("역할 기반 알림 이벤트 수신 시 Outbox에 저장")
    void handleReportEvent_역할알림_Outbox저장() {
        ReportNotificationEvent event = new ReportNotificationEvent(
                Role.ADMIN,
                NotificationType.REPORT_RECEIVED,
                "신고 접수",
                "새 신고가 접수되었습니다",
                5L
        );

        eventListener.handleReportEvent(event);

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        then(outboxRepository).should().save(captor.capture());

        NotificationOutbox saved = captor.getValue();
        assertThat(saved.getTargetRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.REPORT_RECEIVED);
        assertThat(saved.getMessage()).isEqualTo("새 신고가 접수되었습니다");
        assertThat(saved.getReferenceId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("역할 기반 알림 이벤트 - 전체 발송 (targetRole null)")
    void handleReportEvent_전체발송_Outbox저장() {
        ReportNotificationEvent event = new ReportNotificationEvent(
                null,
                NotificationType.REPORT_RECEIVED,
                "전체 공지",
                "내용",
                null
        );

        eventListener.handleReportEvent(event);

        then(outboxRepository).should().save(any(NotificationOutbox.class));
    }
}
