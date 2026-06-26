package com.team04.domain.verification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.dto.response.VerificationResponse;
import com.team04.domain.verification.dto.response.VerificationResultResponse;
import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationAuditLog;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.event.VerificationRequestedEvent;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.repository.VerificationAuditLogRepository;
import com.team04.domain.verification.repository.VerificationResultRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 검증 접수, 비동기 검증 요청 발행, 검증 결과 조회를 담당하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final ProjectVerificationRepository projectVerificationRepository;
    private final VerificationAuditLogRepository auditLogRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final IdeaRepository ideaRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 검증 요청을 접수하기 전 요청자가 아이디어 제안자인지 확인합니다. */
    @Transactional
    public VerificationResponse requestVerification(VerificationRequest request, Long requesterId) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(request.ideaId())
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        if (!idea.getUserId().equals(requesterId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        ProjectVerification verification = projectVerificationRepository.findByIdeaId(request.ideaId())
                .orElseGet(() -> new ProjectVerification(request.ideaId()));
        VerificationStatus currentStatus = verification.getStatus();
        if (currentStatus == VerificationStatus.AI_VERIFYING) {
            throw new CustomException(ErrorCode.VERIFICATION_ALREADY_IN_PROGRESS);
        }
        if (currentStatus != VerificationStatus.DRAFT && currentStatus != VerificationStatus.PENDING_ADMIN_REVIEW) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_STATUS_TRANSITION);
        }
        verification.startAiVerification();
        ProjectVerification saved = projectVerificationRepository.save(verification);
        audit(saved, currentStatus, VerificationStatus.AI_VERIFYING, "검증 요청 접수");
        eventPublisher.publishEvent(new VerificationRequestedEvent(saved.getId(), request));
        return VerificationResponse.of(saved, "검증 중입니다.");
    }

    /** 아이디어 ID로 검증 결과를 조회합니다. */
    @Transactional(readOnly = true)
    public VerificationResponse getVerificationByIdeaId(Long ideaId, Long userId, Role role) {
        if (role == Role.USER) {
            Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                    .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
            if (!idea.getUserId().equals(userId)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        }
        ProjectVerification verification = projectVerificationRepository.findByIdeaId(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
        List<VerificationResultResponse> results = verificationResultRepository.findAllByIdeaId(ideaId)
                .stream()
                .map(VerificationResultResponse::of)
                .toList();
        return VerificationResponse.of(verification, results, null);
    }

    /** 상태 변경 감사 로그를 저장합니다. */
    private void audit(ProjectVerification verification, VerificationStatus previous, VerificationStatus next, String reason) {
        auditLogRepository.save(new VerificationAuditLog(verification.getIdeaId(), previous, next, reason));
    }
}