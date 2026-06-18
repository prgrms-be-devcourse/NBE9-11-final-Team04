package com.team04.domain.notification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.verification.event.NotificationEvent;
import com.team04.global.event.ReportNotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final IdeaRepository ideaRepository;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent notificationEvent){
        Long ideaId = notificationEvent.ideaId();

        Idea idea = ideaRepository.findById(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        Long userId = idea.getUserId();

        notificationService.createNotification(
                userId,
                notificationEvent.notificationType(),
                notificationEvent.title(),
                notificationEvent.message(),
                ideaId
        );
    }

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
