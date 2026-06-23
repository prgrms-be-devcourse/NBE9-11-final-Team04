package com.team04.domain.expert.service;

import com.team04.domain.expert.entity.ExpertProfile;
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
}