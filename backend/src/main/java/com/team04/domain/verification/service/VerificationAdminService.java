package com.team04.domain.verification.service;

import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.dto.response.AdminVerificationResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 검증 장애 목록 조회와 수동 재시도를 담당하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class VerificationAdminService {

    private final ProjectVerificationRepository projectVerificationRepository;
    private final VerificationAuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** PENDING_ADMIN_REVIEW 상태의 검증 장애 목록을 페이지로 조회합니다. */
    @Transactional(readOnly = true)
    public Page<AdminVerificationResponse> getFailures(Pageable pageable) {
        return projectVerificationRepository
                .findAllByStatus(VerificationStatus.PENDING_ADMIN_REVIEW, pageable)
                .map(AdminVerificationResponse::of);
    }

    /** 관리자가 OpenAI 장애 검증 건을 수동으로 재시도합니다. */
    @Transactional
    public void retry(Long verificationId, VerificationRequest request) {
        ProjectVerification verification = getVerification(verificationId);

        // ideaId 일치 여부 검증
        if (!verification.getIdeaId().equals(request.ideaId())) {
            throw new CustomException(ErrorCode.VERIFICATION_IDEA_MISMATCH);
        }

        VerificationStatus previous = verification.getStatus();
        verification.changeStatus(VerificationStatus.AI_VERIFYING);
        audit(verification, previous, VerificationStatus.AI_VERIFYING, "관리자 수동 재시도");
        eventPublisher.publishEvent(new VerificationRequestedEvent(verification.getId(), request));
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
