package com.team04.domain.verification.service;

import com.team04.domain.verification.dto.openai.AiVerificationStructuredResult;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.TrustScore;
import com.team04.domain.verification.entity.VerificationAuditLog;
import com.team04.domain.verification.entity.VerificationCheckCode;
import com.team04.domain.verification.entity.VerificationDecision;
import com.team04.domain.verification.entity.VerificationResult;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.event.NotificationEvent;
import com.team04.domain.verification.properties.VerificationProperties;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.repository.TrustScoreRepository;
import com.team04.domain.verification.repository.VerificationAuditLogRepository;
import com.team04.domain.verification.repository.VerificationResultRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Controller 요청 트랜잭션과 분리된 비동기 AI 검증 처리를 담당하는 서비스입니다. */
@Service
@RequiredArgsConstructor
public class VerificationAsyncProcessor {

    private static final int REVISION_DAYS = 7;

    private final ProjectVerificationRepository projectVerificationRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final VerificationAuditLogRepository auditLogRepository;
    private final TrustScoreRepository trustScoreRepository;
    private final VerificationProperties verificationProperties;
    private final OpenAiVerificationService openAiVerificationService;
    private final ApplicationEventPublisher eventPublisher;

    /** 별도 스레드와 별도 트랜잭션에서 금칙어 사전 및 OpenAI 검증을 수행합니다. */
    @Async("verificationTaskExecutor")
    @Transactional
    public void processAiVerification(Long verificationId, VerificationRequest request) {
        ProjectVerification verification = getVerification(verificationId);
        try {
            if (containsForbiddenKeyword(request)) {
                applyDecision(verification, forbiddenKeywordResult());
                return;
            }
            applyDecision(verification, openAiVerificationService.verify(request));
        } catch (Exception exception) {
            VerificationStatus previous = verification.getStatus();
            verification.changeStatus(VerificationStatus.PENDING_ADMIN_REVIEW);
            audit(verification, previous, VerificationStatus.PENDING_ADMIN_REVIEW, "AI 검증 재시도 소진 또는 장애로 관리자 검토 전환");
        }
    }

    /** AI 판단 결과를 상태, 검증 결과, 신뢰도 점수로 저장합니다. */
    private void applyDecision(ProjectVerification verification, AiVerificationStructuredResult result) {
        VerificationStatus previous = verification.getStatus();
        VerificationStatus next = toStatus(result.decision());
        if (next == VerificationStatus.NEEDS_REVISION) {
            verification.requestRevision(LocalDateTime.now().plusDays(REVISION_DAYS));
        } else {
            verification.changeStatus(next);
        }
        result.checks().forEach(check -> verificationResultRepository.save(new VerificationResult(
                verification.getIdeaId(), check.checkCode(), check.passed(), check.score(), check.reason()
        )));
        updateTrustScore(verification.getIdeaId(), result);
        audit(verification, previous, next, result.reason());
        eventPublisher.publishEvent(new NotificationEvent(verification.getIdeaId(), "검증 상태 변경", result.reason()));
    }

    /** 검증 결과를 기반으로 미구현 항목은 0점 처리한 신뢰도 점수를 저장합니다. */
    private void updateTrustScore(Long ideaId, AiVerificationStructuredResult result) {
        TrustScore trustScore = trustScoreRepository.findByIdeaId(ideaId).orElseGet(() -> new TrustScore(ideaId));
        trustScore.updateScores(averageScore(result, VerificationCheckCode.EXAGGERATED_ADVERTISEMENT, VerificationCheckCode.SIMILAR_SERVICE), averageScore(result, VerificationCheckCode.MILESTONE_SPECIFICITY), 0, 0, 0, false);
        trustScoreRepository.save(trustScore);
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

    /** yml 금칙어 사전에 포함된 표현이 요청 본문에 있는지 확인합니다. */
    private boolean containsForbiddenKeyword(VerificationRequest request) {
        String content = (request.title() + " " + request.description()).toLowerCase();
        return verificationProperties.forbiddenKeywords().stream().map(String::toLowerCase).anyMatch(content::contains);
    }

    /** 금칙어 사전 탐지 결과를 AI 구조화 결과와 동일한 형태로 생성합니다. */
    private AiVerificationStructuredResult forbiddenKeywordResult() {
        return new AiVerificationStructuredResult(VerificationDecision.REJECT, List.of(new AiVerificationStructuredResult.CheckResult(VerificationCheckCode.EXAGGERATED_ADVERTISEMENT, false, 0, "금칙어 사전에 등록된 과대광고 표현이 포함되었습니다.")), "금칙어 사전 기반 1차 필터링에서 반려되었습니다.");
    }

    /** AI 판단 값을 검증 상태 값으로 변환합니다. */
    private VerificationStatus toStatus(VerificationDecision decision) {
        return switch (decision) {
            case PASS -> VerificationStatus.AI_PASSED;
            case NEEDS_REVISION -> VerificationStatus.NEEDS_REVISION;
            case REJECT -> VerificationStatus.REJECTED;
            case PENDING_ADMIN_REVIEW -> VerificationStatus.PENDING_ADMIN_REVIEW;
        };
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
