package com.team04.domain.expert.scheduler;

import com.team04.domain.expert.client.ExternalVerifyClient;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.expert.service.ExpertVerificationService;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.user.entity.Role;
import com.team04.global.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpertVerificationScheduler {

    private final ExpertProfileRepository expertProfileRepository;
    private final ExternalVerifyClient externalVerifyClient;
    private final ExpertVerificationService expertVerificationService;

    private static final int CHUNK_SIZE = 100;
    private static final int SUSPENSION_DEADLINE_DAYS = 7;

    // 매주 월요일 00:00 — BUSINESS_REGISTRATION 재검증 (offset 방식)
    @Scheduled(cron = "${expert.scheduler.reverify-cron:0 0 0 * * MON}")
    public void reverifyBusinessRegistration() {
        log.info("[Scheduler] BUSINESS_REGISTRATION 재검증 시작");

        int offset = 0;
        int suspendedCount = 0;
        int totalCount = 0;

        while (true) {
            List<ExpertProfile> profiles = expertProfileRepository
                    .findActiveBusinessRegistrationProfiles(offset, CHUNK_SIZE);

            if (profiles.isEmpty()) break;

            for (ExpertProfile profile : profiles) {
                try {
                    ExpertVerifyRequest request = new ExpertVerifyRequest(
                            QualificationType.BUSINESS_REGISTRATION,
                            profile.getQualificationNumber(),
                            profile.getStartDate(),
                            profile.getRepresentativeName(),
                            null
                    );

                    boolean valid = externalVerifyClient.verify(request, false); // 트랜잭션 밖에서 외부 API 호출

                    if (!valid) {
                        expertVerificationService.suspendProfile(profile.getId()); // 개별 트랜잭션
                        suspendedCount++;
                    }

                } catch (Exception e) {
                    log.error("[Scheduler] 재검증 실패: expertProfileId={}, error={}",
                            profile.getId(), e.getMessage());
                }
            }

            totalCount += profiles.size();
            if (profiles.size() < CHUNK_SIZE) break;
            offset += CHUNK_SIZE;
        }

        log.info("[Scheduler] BUSINESS_REGISTRATION 재검증 완료: 전체={}, 격리={}", totalCount, suspendedCount);
    }

    // 매년 1월, 7월 1일 00:00 — NATIONAL_QUALIFICATION 재제출 요청 (offset 방식)
    @Scheduled(cron = "${expert.scheduler.national-cron:0 0 0 1 1,7 *}")
    public void requestNationalQualificationReverification() {
        log.info("[Scheduler] NATIONAL_QUALIFICATION 재제출 요청 알림 발송 시작");

        int offset = 0;
        int totalCount = 0;

        while (true) {
            List<ExpertProfile> profiles = expertProfileRepository
                    .findActiveNationalQualificationProfiles(offset, CHUNK_SIZE);

            if (profiles.isEmpty()) break;

            for (ExpertProfile profile : profiles) {
                try {
                    expertVerificationService.sendReverificationNotification(
                            profile.getId(),
                            profile.getUser().getId()
                    ); // 개별 트랜잭션
                } catch (Exception e) {
                    log.error("[Scheduler] 알림 발송 실패: expertProfileId={}, error={}",
                            profile.getId(), e.getMessage());
                }
            }

            totalCount += profiles.size();
            if (profiles.size() < CHUNK_SIZE) break;
            offset += CHUNK_SIZE;
        }

        log.info("[Scheduler] NATIONAL_QUALIFICATION 재제출 요청 알림 발송 완료: 전체={}", totalCount);
    }

    // 매일 00:00 — 격리 후 7일 초과 시 SPONSOR로 변경 (첫 페이지 반복 방식)
    @Scheduled(cron = "${expert.scheduler.demote-cron:0 0 0 * * *}")
    public void demoteExpiredSuspendedExperts() {
        log.info("[Scheduler] 격리 만료 전문가 SPONSOR 변경 시작");

        LocalDateTime deadline = LocalDateTime.now().minusDays(SUSPENSION_DEADLINE_DAYS);
        int demotedCount = 0;

        while (true) {
            // 처리 후 조건에서 제외되므로 항상 첫 페이지 조회
            List<ExpertProfile> profiles = expertProfileRepository
                    .findExpiredSuspendedProfiles(deadline, CHUNK_SIZE);

            if (profiles.isEmpty()) break;

            for (ExpertProfile profile : profiles) {
                try {
                    expertVerificationService.demoteToSponsor(profile.getId());
                    demotedCount++;
                } catch (Exception e) {
                    log.error("[Scheduler] SPONSOR 변경 실패: expertProfileId={}, error={}",
                            profile.getId(), e.getMessage());
                }
            }
        }

        log.info("[Scheduler] 격리 만료 전문가 SPONSOR 변경 완료: 변경={}", demotedCount);
    }
}