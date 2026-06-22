package com.team04.domain.notification.service;

import com.team04.domain.notification.entity.NotificationOutbox;
import com.team04.domain.notification.repository.NotificationOutboxRepository;
import com.team04.global.event.NotificationEvent;
import com.team04.global.event.ReportNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationOutboxRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        outboxRepository.save(NotificationOutbox.forUser(
                event.userId(), event.notificationType(),
                event.title(), event.message(), event.targetId(),
                event.priority()
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReportEvent(ReportNotificationEvent event) {
        outboxRepository.save(NotificationOutbox.forRole(
                event.notifyRole(), event.notificationType(),
                event.title(), event.message(), event.referenceId(),
                event.priority()
        ));
    }
}
