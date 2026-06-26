package com.team04.domain.verification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaBadge;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.notification.entity.NotificationPriority;
import com.team04.domain.notification.entity.NotificationType;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.domain.verification.dto.openai.AiVerificationStructuredResult;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.entity.*;
import com.team04.domain.verification.event.VerificationRequestedEvent;
import com.team04.domain.verification.properties.VerificationProperties;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.repository.TrustScoreRepository;
import com.team04.domain.verification.repository.VerificationAuditLogRepository;
import com.team04.domain.verification.repository.VerificationResultRepository;
import com.team04.global.event.NotificationEvent;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.regex.Pattern;

/** Controller 요청 트랜잭션과 분리된 비동기 AI 검증 처리를 담당하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class VerificationAsyncProcessor {

    private final ProjectVerificationRepository projectVerificationRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final VerificationAuditLogRepository auditLogRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final IdeaRepository ideaRepository;
    private final VerificationProperties verificationProperties;
    private final OpenAiVerificationService openAiVerificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    private List<Pattern> forbiddenKeywordPatterns;

    /** 스프링 빈 초기화 후 금칙어 정규식 패턴을 한 번만 컴파일합니다. */
    @PostConstruct
    public void initializeForbiddenKeywordPatterns() {
        forbiddenKeywordPatterns = verificationProperties.forbiddenKeywords().stream()
                .map(keyword -> keyword.replace(" ", "\\s*"))
                .map(keyword -> Pattern.compile(keyword, Pattern.CASE_INSENSITIVE))
                .toList();
    }

    /** 트랜잭션 커밋 후 별도 스레드에서 금칙어 사전 및 OpenAI 검증을 수행합니다. */
    @Async("verificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAiVerification(VerificationRequestedEvent event) {
        transactionTemplate.executeWithoutResult(status -> processAiVerificationInTransaction(event));
    }

    /** 비동기 리스너와 분리된 트랜잭션 경계에서 1차 금칙어 탐지와 2차 AI 검증을 수행합니다. */
    @Transactional
    public void processAiVerificationInTransaction(VerificationRequestedEvent event) {
        ProjectVerification verification = getVerification(event.verificationId());
        if (verification.getStatus() == VerificationStatus.CANCELLED) {
            return;
        }
        try {
            AiVerificationStructuredResult aiResult = openAiVerificationService.verify(event.request());
            applyReferenceResults(verification, mergeResults(detectForbiddenKeywords(event.request()), aiResult));
        } catch (Exception exception) {
            VerificationStatus previous = verification.getStatus();
            verification.markPendingAdminReview();
            audit(verification, previous, VerificationStatus.PENDING_ADMIN_REVIEW,
                    "AI 검증 호출 실패로 관리자 재시도 필요: " + exception.getMessage());
            publishPendingAdminReviewNotification(verification.getIdeaId());
        }
    }

    /** 1차 금칙어 탐지 결과와 2차 AI 검증 결과를 저장하고 아이디어를 전문가 심사 대기로 전이합니다. */
    private void applyReferenceResults(ProjectVerification verification, AiVerificationStructuredResult result) {
        VerificationStatus previous = verification.getStatus();
        verification.completeAiVerification();
        result.checks().forEach(check -> verificationResultRepository.save(new VerificationResult(
                verification.getIdeaId(), check.checkCode(), check.passed(), check.score(), check.reason()
        )));
        Idea idea = updateIdeaAfterAiVerification(verification.getIdeaId(), result);
        audit(verification, previous, VerificationStatus.AI_PASSED, result.reason());
        publishProposerNotification(idea);
    }

    /** AI 검증 완료 후 아이디어 상태와 신뢰도 점수를 반영합니다. */
    private Idea updateIdeaAfterAiVerification(Long ideaId, AiVerificationStructuredResult result) {
        TrustScore trustScore = updateTrustScore(ideaId, result);
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId)
                .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));

        if (idea.getStatus() == IdeaStatus.AI_PENDING) {
            idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        }

        if (trustScore.getTotalScore() >= 80) {
            idea.changeBadge(IdeaBadge.VERIFIED);
        }

        return ideaRepository.save(idea);
    }

    /** 참고용 AI 검증 결과를 아이디어 제안자에게 알립니다. */
    private void publishProposerNotification(Idea idea) {
        eventPublisher.publishEvent(new NotificationEvent(
                idea.getUserId(),
                NotificationType.IDEA_AI_APPROVED,
                "AI 검증 결과 안내",
                "아이디어 참고용 검증 결과가 저장되었고 전문가 심사 단계로 이동했습니다.",
                idea.getId(),
                NotificationPriority.NORMAL
        ));
    }

    /** 관리자 재시도가 필요한 검증 장애를 모든 활성 관리자에게 알립니다. */
    private void publishPendingAdminReviewNotification(Long ideaId) {
        userRepository.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE)
                .forEach(admin -> eventPublisher.publishEvent(new NotificationEvent(
                        admin.getId(),
                        NotificationType.ANNOUNCEMENT,
                        "AI 검증 장애 발생",
                        "OpenAI 검증 호출에 실패한 아이디어가 있습니다. 관리자 재시도가 필요합니다.",
                        ideaId,
                        NotificationPriority.CRITICAL
                )));
    }

    /** 검증 결과를 기반으로 미구현 항목은 0점 처리한 신뢰도 점수를 저장합니다. */
    private TrustScore updateTrustScore(Long ideaId, AiVerificationStructuredResult result) {
        TrustScore trustScore = trustScoreRepository.findByIdeaId(ideaId)
                .orElseGet(() -> new TrustScore(ideaId));
        trustScore.updateScores(
                averageScore(
                        result,
                        VerificationCheckCode.EXAGGERATED_ADVERTISEMENT,
                        VerificationCheckCode.SIMILAR_SERVICE
                ),
                averageScore(result, VerificationCheckCode.MILESTONE_SPECIFICITY),
                0,
                0,
                0
        );
        return trustScoreRepository.save(trustScore);
    }

    /** 지정한 검증 항목들의 평균 점수를 계산합니다. */
    private int averageScore(AiVerificationStructuredResult result, VerificationCheckCode... codes) {
        int sum = 0;
        int count = 0;
        for (VerificationCheckCode code : codes) {
            for (AiVerificationStructuredResult.CheckResult check : result.checks()) {
                if (check.checkCode() == code) {
                    sum += check.score();
                    count++;
                }
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    /** yml 금칙어 사전에 포함된 표현을 검증 결과 항목으로 변환합니다. */
    private List<AiVerificationStructuredResult.CheckResult> detectForbiddenKeywords(VerificationRequest request) {
        String content = request.title() + " " + request.description();
        return forbiddenKeywordPatterns.stream()
                .filter(pattern -> pattern.matcher(content).find())
                .map(pattern -> new AiVerificationStructuredResult.CheckResult(
                        VerificationCheckCode.EXAGGERATED_ADVERTISEMENT,
                        false,
                        0,
                        "금칙어 사전에 등록된 표현이 포함되었습니다."
                ))
                .toList();
    }

    /** 1차 금칙어 탐지 결과와 2차 AI 검증 결과를 하나의 구조화 결과로 병합합니다. */
    private AiVerificationStructuredResult mergeResults(
            List<AiVerificationStructuredResult.CheckResult> forbiddenKeywordResults,
            AiVerificationStructuredResult aiResult
    ) {
        List<AiVerificationStructuredResult.CheckResult> mergedChecks = new java.util.ArrayList<>();
        mergedChecks.addAll(forbiddenKeywordResults);
        mergedChecks.addAll(aiResult.checks());
        String reason = forbiddenKeywordResults.isEmpty()
                ? aiResult.reason()
                : "금칙어 탐지 결과와 AI 검증 결과를 참고용으로 저장했습니다. " + aiResult.reason();
        return new AiVerificationStructuredResult(aiResult.decision(), mergedChecks, reason);
    }

    /** 검증 엔티티를 조회하고 없으면 예외를 발생시킵니다. */
    private ProjectVerification getVerification(Long verificationId) {
        return projectVerificationRepository.findById(verificationId).orElseThrow(() -> new CustomException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    /** 상태 변경 감사 로그를 저장합니다. */
    private void audit(ProjectVerification verification, VerificationStatus previous, VerificationStatus next, String reason) {
        auditLogRepository.save(new VerificationAuditLog(verification.getIdeaId(), previous, next, reason));
    }
}
