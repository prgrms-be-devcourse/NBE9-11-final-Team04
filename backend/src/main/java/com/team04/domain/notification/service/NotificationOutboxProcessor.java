package com.team04.domain.notification.service;

import com.team04.domain.notification.entity.NotificationOutbox;
import com.team04.domain.notification.entity.NotificationOutboxStatus;
import com.team04.domain.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxProcessor {

    private static final int MAX_RETRY = 3;
    private static final int BATCH_SIZE = 50;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<Long> findPendingIds() {
        return outboxRepository.findProcessable(
                        NotificationOutboxStatus.PENDING, MAX_RETRY, LocalDateTime.now(),
                        PageRequest.of(0, BATCH_SIZE))
                .stream()
                .map(NotificationOutbox::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(Long outboxId) {
        NotificationOutbox entry = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new IllegalStateException("outbox entry not found: " + outboxId));

        try {
            if (entry.isRoleBased()) {
                notificationService.createAnnouncementToRole(
                        entry.getTargetRole(), entry.getNotificationType(),
                        entry.getTitle(), entry.getMessage(), entry.getReferenceId()
                );
            } else {
                notificationService.createNotification(
                        entry.getUserId(), entry.getNotificationType(),
                        entry.getTitle(), entry.getMessage(), entry.getReferenceId()
                );
            }
            entry.markSent();
        } catch (Exception e) {
            entry.incrementRetryOrFail();
            log.error("[Outbox] 알림 처리 실패 id={}, priority={}, retryCount={}",
                    outboxId, entry.getPriority(), entry.getRetryCount(), e);
        }
    }
}
