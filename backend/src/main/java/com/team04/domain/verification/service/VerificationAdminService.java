package com.team04.domain.verification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.notification.entity.NotificationPriority;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.verification.dto.response.AdminVerificationResponse;
import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationAuditLog;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.repository.VerificationAuditLogRepository;
import com.team04.global.event.NotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 검증 검토 목록 조회, 승인, 반려를 담당하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class VerificationAdminService {

    private final ProjectVerificationRepository projectVerificationRepository;
    private final VerificationAuditLogRepository auditLogRepository;
    private final IdeaRepository ideaRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** PENDING_ADMIN_REVIEW 상태의 검증 목록을 페이지로 조회합니다. */
    @Transactional(readOnly = true)
    public Page<AdminVerificationResponse> getReviews(Pageable pageable) {
        return projectVerificationRepository
                .findAllByStatus(VerificationStatus.PENDING_ADMIN_REVIEW, pageable)
                .map(AdminVerificationResponse::of);
    }

    /** 관리자가 검증을 통과 처리하고 아이디어 상태를 전문가 매칭 대기로 전이합니다. */
    @Transactional
    public void pass(Long verificationId) {
        ProjectVerification verification = getVerification(verificationId);
        VerificationStatus previous = verification.getStatus();
        verification.changeStatus(VerificationStatus.AI_PASSED);
        audit(verification, previous, VerificationStatus.AI_PASSED, "관리자 검증 통과 처리");

        Idea idea = getIdea(verification.getIdeaId());
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        ideaRepository.save(idea);

        eventPublisher.publishEvent(new NotificationEvent(
                idea.getUserId(),
                NotificationType.IDEA_AI_APPROVED,
                "AI 검증 통과",
                "관리자 검토 결과 아이디어가 검증을 통과했습니다.",
                idea.getId(),
                NotificationPriority.NORMAL
        ));
    }

    /** 관리자가 검증을 반려 처리하고 아이디어 상태를 반려로 전이합니다. */
    @Transactional
    public void reject(Long verificationId, String reason) {
        ProjectVerification verification = getVerification(verificationId);
        VerificationStatus previous = verification.getStatus();
        verification.changeStatus(VerificationStatus.REJECTED);
        audit(verification, previous, VerificationStatus.REJECTED, reason);

        Idea idea = getIdea(verification.getIdeaId());
        idea.changeStatus(IdeaStatus.REJECTED);
        ideaRepository.save(idea);

        eventPublisher.publishEvent(new NotificationEvent(
                idea.getUserId(),
                NotificationType.IDEA_AI_REJECTED,
                "AI 검증 반려",
                "관리자 검토 결과 아이디어가 반려되었습니다. 사유: " + reason,
                idea.getId(),
                NotificationPriority.NORMAL
        ));
    }

    private ProjectVerification getVerification(Long verificationId) {
        return projectVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    private Idea getIdea(Long ideaId) {
        return ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
    }

    private void audit(ProjectVerification verification, VerificationStatus previous, VerificationStatus next, String reason) {
        auditLogRepository.save(new VerificationAuditLog(verification.getIdeaId(), previous, next, reason));
    }
}
