package com.team04.domain.expert.scheduler;

import com.team04.domain.expert.client.ExternalVerifyClient;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertProfileRepository;
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
    private final ApplicationEventPublisher eventPublisher;

    private static final int SUSPENSION_DEADLINE_DAYS = 7;

    // 매주 월요일 00:00 — BUSINESS_REGISTRATION 재검증
    // @Scheduled(cron = "0 0 0 * * MON")
    @Scheduled(cron = "${expert.scheduler.reverify-cron}")
    @Transactional
    public void reverifyBusinessRegistration() {
        log.info("[Scheduler] BUSINESS_REGISTRATION 재검증 시작");

        List<ExpertProfile> profiles = expertProfileRepository.findActiveBusinessRegistrationProfiles();
        int suspendedCount = 0;

        for (ExpertProfile profile : profiles) {
            try {
                ExpertVerifyRequest request = new ExpertVerifyRequest(
                        QualificationType.BUSINESS_REGISTRATION,
                        profile.getQualificationNumber(),
                        profile.getStartDate(),
                        profile.getRepresentativeName(),
                        null
                );

                boolean valid = externalVerifyClient.verify(request, false);

                if (!valid) {
                    suspendProfile(profile, NotificationType.EXPERT_SUSPENDED,
                            "전문가 자격 검증 실패",
                            "사업자 상태 확인 결과 자격이 유지되지 않아 계정이 격리되었습니다. 7일 이내 소명 자료를 제출해 주세요.");
                    suspendedCount++;
                }

            } catch (Exception e) {
                log.error("[Scheduler] BUSINESS_REGISTRATION 재검증 실패: expertProfileId={}, error={}",
                        profile.getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] BUSINESS_REGISTRATION 재검증 완료: 전체={}, 격리={}", profiles.size(), suspendedCount);
    }

    // 매년 1월, 7월 1일 00:00 — NATIONAL_QUALIFICATION 재제출 요청
    // @Scheduled(cron = "0 0 0 1 1,7 *")
    @Scheduled(cron = "${expert.scheduler.national-cron}")
    @Transactional
    public void requestNationalQualificationReverification() {
        log.info("[Scheduler] NATIONAL_QUALIFICATION 재제출 요청 알림 발송 시작");

        List<ExpertProfile> profiles = expertProfileRepository.findActiveNationalQualificationProfiles();

        for (ExpertProfile profile : profiles) {
            try {
                eventPublisher.publishEvent(new NotificationEvent(
                        profile.getUser().getId(),
                        NotificationType.EXPERT_REVERIFICATION_REQUIRED,
                        "국가자격증 서류 재제출 요청",
                        "자격증 유효성 확인을 위해 7일 이내 자격증 확인증을 재제출해 주세요. 미제출 시 계정이 격리됩니다.",
                        profile.getId()
                ));
            } catch (Exception e) {
                log.error("[Scheduler] NATIONAL_QUALIFICATION 알림 발송 실패: expertProfileId={}, error={}",
                        profile.getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] NATIONAL_QUALIFICATION 재제출 요청 알림 발송 완료: 전체={}", profiles.size());
    }

    // 매일 00:00 — 격리 후 7일 초과 시 SPONSOR로 변경
    // @Scheduled(cron = "0 0 0 * * *")
    @Scheduled(cron = "${expert.scheduler.demote-cron}")
    @Transactional
    public void demoteExpiredSuspendedExperts() {
        log.info("[Scheduler] 격리 만료 전문가 SPONSOR 변경 시작");

        LocalDateTime deadline = LocalDateTime.now().minusDays(SUSPENSION_DEADLINE_DAYS);
        List<ExpertProfile> profiles = expertProfileRepository.findExpiredSuspendedProfiles(deadline);
        int demotedCount = 0;

        for (ExpertProfile profile : profiles) {
            try {
                profile.getUser().changeRole(Role.SPONSOR);
                log.info("[Scheduler] SPONSOR 변경 완료: userId={}", profile.getUser().getId());
                demotedCount++;
            } catch (Exception e) {
                log.error("[Scheduler] SPONSOR 변경 실패: expertProfileId={}, error={}",
                        profile.getId(), e.getMessage());
            }
        }

        log.info("[Scheduler] 격리 만료 전문가 SPONSOR 변경 완료: 전체={}, 변경={}", profiles.size(), demotedCount);
    }

    private void suspendProfile(ExpertProfile profile, NotificationType type, String title, String message) {
        profile.suspend();
        eventPublisher.publishEvent(new NotificationEvent(
                profile.getUser().getId(),
                type,
                title,
                message,
                profile.getId()
        ));
        log.info("[Scheduler] 계정 격리 처리: expertProfileId={}, userId={}", profile.getId(), profile.getUser().getId());
    }
}