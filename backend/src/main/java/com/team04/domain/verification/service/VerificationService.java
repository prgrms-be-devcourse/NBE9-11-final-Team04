package com.team04.domain.verification.service;

import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.dto.response.VerificationResponse;
import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationAuditLog;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.event.VerificationRequestedEvent;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.repository.VerificationAuditLogRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 검증 접수, 재제출, 비동기 AI 검증, 보완 기한 관리를 담당하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final int RESUBMISSION_WAIT_THRESHOLD = 3;
    private static final int WAITING_DAYS = 30;

    private final ProjectVerificationRepository projectVerificationRepository;
    private final VerificationAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 검증 요청을 접수하고 Controller 트랜잭션 종료 후 백그라운드 검증을 시작합니다. */
    @Transactional
    public VerificationResponse requestVerification(VerificationRequest request) {
        ProjectVerification verification = projectVerificationRepository.findByIdeaId(request.ideaId())
                .orElseGet(() -> new ProjectVerification(request.ideaId()));
        blockIfWaiting(verification);
        VerificationStatus currentStatus = verification.getStatus();
        if (currentStatus == VerificationStatus.NEEDS_REVISION) {
            throw new CustomException(ErrorCode.USE_RESUBMIT_API);
        }
        if (currentStatus == VerificationStatus.AI_VERIFYING ||
                currentStatus == VerificationStatus.PENDING_ADMIN_REVIEW) {
            throw new CustomException(ErrorCode.VERIFICATION_ALREADY_IN_PROGRESS);
        }
        verification.startAiVerification();
        ProjectVerification saved = projectVerificationRepository.save(verification);
        audit(saved, VerificationStatus.DRAFT, VerificationStatus.AI_VERIFYING, "검증 요청 접수");
        eventPublisher.publishEvent(new VerificationRequestedEvent(saved.getId(), request));
        return VerificationResponse.of(saved, "검증 중입니다.");
    }

    /** 보완 대상 검증 건을 재제출하고 재제출 제한과 대기 기간을 적용합니다. */
    @Transactional
    public VerificationResponse resubmit(Long verificationId, VerificationRequest request) {
        ProjectVerification verification = getVerification(verificationId);
        blockIfWaiting(verification);
        if (verification.getResubmissionCount() >= RESUBMISSION_WAIT_THRESHOLD) {
            VerificationStatus previous = verification.getStatus();
            verification.rejectWithWaiting(LocalDateTime.now().plusDays(WAITING_DAYS));
            audit(verification, previous, VerificationStatus.REJECTED, "재제출 3회 초과로 30일 대기");
            return VerificationResponse.of(verification, "재제출 횟수 초과로 30일 대기 상태입니다.");
        }
        VerificationStatus previous = verification.getStatus();
        verification.resubmit();
        audit(verification, previous, VerificationStatus.AI_VERIFYING, "보완안 재제출");
        eventPublisher.publishEvent(new VerificationRequestedEvent(verification.getId(), request));
        return VerificationResponse.of(verification, "검증 중입니다.");
    }

    /** 매일 새벽 보완 기한이 지난 검증 요청을 자동 반려합니다. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void rejectExpiredRevisionRequests() {
        projectVerificationRepository.findAllByStatusAndRevisionDueAtBefore(
                VerificationStatus.NEEDS_REVISION,
                LocalDateTime.now()
        ).forEach(verification -> {
            verification.changeStatus(VerificationStatus.REJECTED);
            audit(verification, VerificationStatus.NEEDS_REVISION, VerificationStatus.REJECTED, "수정 기한 7일 초과 자동 반려");
        });
    }

    /** 대기 기간 중 재등록 시도를 차단합니다. */
    private void blockIfWaiting(ProjectVerification verification) {
        if (verification.getWaitingUntil() != null && verification.getWaitingUntil().isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.VERIFICATION_WAITING_PERIOD_ACTIVE);
        }
    }

    /** 검증 엔티티를 조회하고 없으면 예외를 발생시킵니다. */
    private ProjectVerification getVerification(Long verificationId) {
        return projectVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    /** 상태 변경 감사 로그를 저장합니다. */
    private void audit(ProjectVerification verification, VerificationStatus previous, VerificationStatus next, String reason) {
        auditLogRepository.save(new VerificationAuditLog(verification.getIdeaId(), previous, next, reason));
    }
}