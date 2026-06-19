package com.team04.domain.notification.service;

import com.team04.domain.notification.entity.NotificationType;
import com.team04.global.event.NotificationEvent;
import com.team04.global.event.ReportNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    //대상에게 알림
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event){
        notificationService.createNotification(
                event.userId(), event.notificationType(),
                event.title(), event.message(), event.targetId()
        );
    }

    //관리자 모두에게 알림
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportEvent(ReportNotificationEvent event) {
        notificationService.createNotificationsToAdmins(
                NotificationType.REPORT_RECEIVED,
                event.targetType() + " 신고 접수",
                "신고 사유: " + event.reason(),
                event.targetId()
        );
    }
}
