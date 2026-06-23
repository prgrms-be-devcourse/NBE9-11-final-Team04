package com.team04.infra.batch;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.notification.service.NotificationService;
import com.team04.domain.user.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final int CLOSING_SOON_DAYS = 7;

    private final IdeaRepository ideaRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 0 * * *")
    public void sendClosingSoonNotifications() {
        log.info("마감임박 알림 스케줄러 시작");

        LocalDateTime from = LocalDate.now().plusDays(CLOSING_SOON_DAYS).atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(CLOSING_SOON_DAYS + 1).atStartOfDay();

        List<Idea> closingIdeas = ideaRepository.findClosingIdeas(IdeaStatus.OPEN, from, to);

        for (Idea idea : closingIdeas) {
            try {
                notifyProposer(idea);
                notifyAllSponsors(idea);
            } catch (Exception e) {
                log.error("마감임박 알림 실패 - ideaId: {}, error: {}", idea.getId(), e.getMessage());
            }
        }

        log.info("마감임박 알림 스케줄러 종료 - 대상 프로젝트 수: {}", closingIdeas.size());
    }

    private void notifyProposer(Idea idea) {
        notificationService.createNotification(
                idea.getUserId(),
                NotificationType.IDEA_FUNDING_CLOSING_SOON,
                "펀딩 마감 임박",
                "'" + idea.getTitle() + "' 펀딩이 7일 후 마감됩니다.",
                idea.getId()
        );
    }

    private void notifyAllSponsors(Idea idea) {
        notificationService.createAnnouncementToRole(
                Role.SPONSOR,
                NotificationType.IDEA_FUNDING_CLOSING_SOON,
                "마감 임박 프로젝트 안내",
                "'" + idea.getTitle() + "' 프로젝트가 7일 후 마감됩니다. 지금 후원해보세요!",
                idea.getId()
        );
    }
}
