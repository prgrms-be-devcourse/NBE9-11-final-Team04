package com.team04.domain.verification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaBadge;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationAsyncProcessorTest {

    @Mock
    private ProjectVerificationRepository projectVerificationRepository;
    @Mock
    private VerificationResultRepository verificationResultRepository;
    @Mock
    private VerificationAuditLogRepository auditLogRepository;
    @Mock
    private TrustScoreRepository trustScoreRepository;
    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private VerificationProperties verificationProperties;
    @Mock
    private OpenAiVerificationService openAiVerificationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private VerificationAsyncProcessor processor;

    @BeforeEach
    void setUp() {
        given(verificationProperties.forbiddenKeywords()).willReturn(List.of("무조건 성공", "원금 보장"));
        processor.initializeForbiddenKeywordPatterns();
        doAnswer(invocation -> {
            invocation.getArgument(0, TransactionCallback.class).doInTransaction(null);
            return null;
        }).when(transactionTemplate).execute(any());
        doAnswer(invocation -> {
            ((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)
                    invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        given(userRepository.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE)).willReturn(List.of());
    }

    private ProjectVerification aiVerifyingVerification() {
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        return verification;
    }

    private VerificationRequest request(String title, String description) {
        return new VerificationRequest(
                1L,
                title,
                description,
                List.of(new VerificationRequest.MilestoneInfo(
                        "목표",
                        "결과물",
                        LocalDate.now().plusMonths(1),
                        10000L
                ))
        );
    }

    private Idea idea() {
        return new Idea(
                1L,
                "아이디어",
                IdeaCategory.TECH,
                "한 줄 소개",
                "문제 정의",
                "해결책",
                "목표",
                "고객",
                "경쟁사",
                "팀 소개",
                100000L,
                0L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusMonths(1),
                RewardType.REWARD_POINT,
                null,
                null
        );
    }

    @Test
    @DisplayName("AI 검증 통과 처리 성공")
    void processAiVerification_AI통과성공() {
        ProjectVerification verification = aiVerifyingVerification();
        AiVerificationStructuredResult result = new AiVerificationStructuredResult(
                VerificationDecision.PASS,
                List.of(
                        new AiVerificationStructuredResult.CheckResult(
                                VerificationCheckCode.EXAGGERATED_ADVERTISEMENT, true, 20, "과대광고 없음"),
                        new AiVerificationStructuredResult.CheckResult(
                                VerificationCheckCode.SIMILAR_SERVICE, true, 18, "유사 서비스 차별점 있음"),
                        new AiVerificationStructuredResult.CheckResult(
                                VerificationCheckCode.MILESTONE_SPECIFICITY, true, 16, "마일스톤 구체적")
                ),
                "AI 검증 통과"
        );
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willReturn(result);
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(trustScoreRepository.save(any(TrustScore.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea()));
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> invocation.getArgument(0));

        processor.processAiVerification(new VerificationRequestedEvent(null, request("정상 제목", "정상 설명")));

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.AI_PASSED);
        then(verificationResultRepository).should(times(3)).save(any(VerificationResult.class));
        then(trustScoreRepository).should().save(any(TrustScore.class));
    }

    @Test
    @DisplayName("금칙어 포함 검증 요청 반려 성공")
    void processAiVerification_금칙어반려성공() {
        ProjectVerification verification = aiVerifyingVerification();
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(trustScoreRepository.save(any(TrustScore.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea()));
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> invocation.getArgument(0));

        processor.processAiVerification(new VerificationRequestedEvent(null, request("무조건 성공 서비스", "정상 설명")));

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        then(openAiVerificationService).should(never()).verify(any());
        then(verificationResultRepository).should().save(any(VerificationResult.class));
    }

    @Test
    @DisplayName("보완 필요 판단 시 보완 기한 저장 성공")
    void processAiVerification_보완필요성공() {
        ProjectVerification verification = aiVerifyingVerification();
        AiVerificationStructuredResult result = new AiVerificationStructuredResult(
                VerificationDecision.NEEDS_REVISION,
                List.of(new AiVerificationStructuredResult.CheckResult(
                        VerificationCheckCode.MILESTONE_SPECIFICITY, false, 10, "마일스톤 보완 필요")),
                "보완 필요"
        );
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willReturn(result);
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(trustScoreRepository.save(any(TrustScore.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea()));
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> invocation.getArgument(0));

        processor.processAiVerification(new VerificationRequestedEvent(null, request("정상 제목", "정상 설명")));

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.NEEDS_REVISION);
        assertThat(verification.getRevisionDueAt()).isAfter(LocalDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("신뢰도 총점 80점 이상 시 인증 배지 부여 성공")
    void processAiVerification_인증배지부여성공() {
        ProjectVerification verification = aiVerifyingVerification();
        Idea idea = idea();
        TrustScore savedHighTrustScore = new TrustScore(1L);
        savedHighTrustScore.updateScores(20, 20, 20, 20, 20);
        AiVerificationStructuredResult result = new AiVerificationStructuredResult(
                VerificationDecision.PASS,
                List.of(new AiVerificationStructuredResult.CheckResult(
                        VerificationCheckCode.MILESTONE_SPECIFICITY, true, 20, "마일스톤 구체적")),
                "AI 검증 통과"
        );
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willReturn(result);
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(trustScoreRepository.save(any(TrustScore.class))).willReturn(savedHighTrustScore);
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea));
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> invocation.getArgument(0));

        processor.processAiVerification(new VerificationRequestedEvent(null, request("정상 제목", "정상 설명")));

        assertThat(idea.getBadge()).isEqualTo(IdeaBadge.VERIFIED);
        then(ideaRepository).should().save(idea);
    }

    @Test
    @DisplayName("AI 검증 장애 시 관리자 검토 전환 성공")
    void processAiVerification_AI장애관리자검토성공() {
        ProjectVerification verification = aiVerifyingVerification();
        ArgumentCaptor<VerificationAuditLog> captor = ArgumentCaptor.forClass(VerificationAuditLog.class);
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willThrow(new RuntimeException("AI 장애"));

        processor.processAiVerification(new VerificationRequestedEvent(null, request("정상 제목", "정상 설명")));

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.PENDING_ADMIN_REVIEW);
        then(auditLogRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNextStatus()).isEqualTo(VerificationStatus.PENDING_ADMIN_REVIEW);
    }
}