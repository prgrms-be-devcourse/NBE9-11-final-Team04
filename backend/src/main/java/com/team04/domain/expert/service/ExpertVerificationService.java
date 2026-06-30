package com.team04.domain.expert.service;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.user.entity.Role;
import com.team04.global.event.NotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertVerificationService {

    private final ExpertProfileRepository expertProfileRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void suspendProfile(Long expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        profile.suspend();

        eventPublisher.publishEvent(new NotificationEvent(
                profile.getUser().getId(),
                NotificationType.EXPERT_SUSPENDED,
                "전문가 자격 검증 실패",
                "사업자 상태 확인 결과 자격이 유지되지 않아 계정이 격리되었습니다. 7일 이내 소명 자료를 제출해 주세요.",
                profile.getId()
        ));

        log.info("[ExpertVerificationService] 계정 격리 처리 완료: expertProfileId={}, userId={}",
                expertProfileId, profile.getUser().getId());
    }

    @Transactional
    public void demoteToSponsor(Long expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        profile.getUser().changeRole(Role.USER);
        profile.demote(); // DEMOTED 상태로 변경

        log.info("[ExpertVerificationService] USER 변경 완료: expertProfileId={}, userId={}",
                expertProfileId, profile.getUser().getId());
    }

    @Transactional
    public void sendReverificationNotification(Long expertProfileId, Long userId) {
        eventPublisher.publishEvent(new NotificationEvent(
                userId,
                NotificationType.EXPERT_REVERIFICATION_REQUIRED,
                "국가자격증 서류 재제출 요청",
                "자격증 유효성 확인을 위해 7일 이내 자격증 확인증을 재제출해 주세요. 미제출 시 계정이 격리됩니다.",
                expertProfileId
        ));
    }

    @Transactional
    public void restoreProfile(Long expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        if (profile.getStatus() != ExpertStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.EXPERT_NOT_SUSPENDED);
        }

        profile.restore();

        eventPublisher.publishEvent(new NotificationEvent(
                profile.getUser().getId(),
                NotificationType.EXPERT_RESTORED,
                "전문가 계정 복구 완료",
                "소명 자료 검토 결과 계정이 복구되었습니다.",
                profile.getId()
        ));

        log.info("[ExpertVerificationService] 계정 복구 완료: expertProfileId={}", expertProfileId);
    }

    @Transactional
    public void approvePendingProfile(Long expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        if (profile.getStatus() != ExpertStatus.PENDING_VERIFICATION) {
            throw new CustomException(ErrorCode.EXPERT_NOT_PENDING);
        }

        profile.verify(); // verified=true, ACTIVE 전환

        eventPublisher.publishEvent(new NotificationEvent(
                profile.getUser().getId(),
                NotificationType.EXPERT_VERIFICATION_APPROVED,
                "전문가 자격 승인 완료",
                "국가자격증 검토 결과 전문가 자격이 승인되었습니다. 프로필을 등록하고 활동을 시작해 주세요.",
                profile.getId()
        ));

        log.info("[ExpertVerificationService] 국가자격증 수동 승인 완료: expertProfileId={}", expertProfileId);
    }

    @Transactional
    public void rejectPendingProfile(Long expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        if (profile.getStatus() != ExpertStatus.PENDING_VERIFICATION) {
            throw new CustomException(ErrorCode.EXPERT_NOT_PENDING);
        }

        Long userId = profile.getUser().getId();

        expertProfileRepository.delete(profile); // 프로필 삭제

        eventPublisher.publishEvent(new NotificationEvent(
                userId,
                NotificationType.EXPERT_VERIFICATION_REJECTED,
                "전문가 자격 거절",
                "국가자격증 검토 결과 자격 요건이 충족되지 않아 거절되었습니다. 서류를 확인 후 재신청해 주세요.",
                expertProfileId
        ));

        log.info("[ExpertVerificationService] 국가자격증 수동 거절 완료: expertProfileId={}", expertProfileId);
    }
}