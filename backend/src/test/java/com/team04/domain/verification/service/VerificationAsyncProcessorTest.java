package com.team04.domain.verification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaBadge;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.entity.MatchStatus;
import com.team04.domain.match.repository.ExpertMatchRepository;
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
    private ExpertMatchRepository expertMatchRepository;
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
    @Mock
    private ProposerHistoryScoreCalculator proposerHistoryScoreCalculator;

    @InjectMocks
    private VerificationAsyncProcessor processor;

    @BeforeEach
    void setUp() {
        given(verificationProperties.forbiddenKeywords()).willReturn(List.of("무조건 성공", "원금 보장"));
        processor.initializeForbiddenKeywordPatterns();

        // execute() - Boolean 반환 (취소 여부 확인용)
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());

        // executeWithoutResult() - 결과 저장/실패 처리용
        lenient().doAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallbackWithoutResult callback =
                    new org.springframework.transaction.support.TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(org.springframework.transaction.TransactionStatus status) {
                            ((java.util.function.Consumer<org.springframework.transaction.TransactionStatus>)
                                    invocation.getArgument(0)).accept(status);
                        }
                    };
            callback.doInTransaction(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        lenient().when(userRepository.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE)).thenReturn(List.of());
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
                VerificationDecision.INFO,
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
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willReturn(result);
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(trustScoreRepository.save(any(TrustScore.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ideaRepository.findByIdForUpdate(1L)).willReturn(Optional.of(idea()));
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(expertMatchRepository.existsByIdeaIdAndStatus(1L, MatchStatus.ACCEPTED)).willReturn(false);

        processor.processAiVerification(new VerificationRequestedEvent(null, request("정상 제목", "정상 설명")));

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.AI_PASSED);
        then(verificationResultRepository).should().saveAll(any());
        then(trustScoreRepository).should().save(any(TrustScore.class));
    }

    @Test
    @DisplayName("금칙어 포함 검증 요청도 AI 검증 결과와 합쳐 저장 성공")
    void processAiVerification_금칙어참고결과저장성공() {
        ProjectVerification verification = aiVerifyingVerification();
        AiVerificationStructuredResult result = new AiVerificationStructuredResult(
                VerificationDecision.INFO,
                List.of(new AiVerificationStructuredResult.CheckResult(
                        VerificationCheckCode.MILESTONE_SPECIFICITY, true, 16, "마일스톤 구체적")),
                "AI 검증 완료"
        );
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willReturn(result);
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(trustScoreRepository.save(any(TrustScore.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ideaRepository.findByIdForUpdate(1L)).willReturn(Optional.of(idea()));
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(expertMatchRepository.existsByIdeaIdAndStatus(1L, MatchStatus.ACCEPTED)).willReturn(false);

        processor.processAiVerification(new VerificationRequestedEvent(null, request("무조건 성공 서비스", "정상 설명")));

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.AI_PASSED);
        then(openAiVerificationService).should().verify(any());
        then(verificationResultRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("신뢰도 총점 80점 이상 시 인증 배지 부여 성공")
    void processAiVerification_인증배지부여성공() {
        ProjectVerification verification = aiVerifyingVerification();
        Idea idea = idea();
        TrustScore savedHighTrustScore = new TrustScore(1L);
        savedHighTrustScore.updateScores(20, 20, 20, 20, 20);
        AiVerificationStructuredResult result = new AiVerificationStructuredResult(
                VerificationDecision.INFO,
                List.of(new AiVerificationStructuredResult.CheckResult(
                        VerificationCheckCode.MILESTONE_SPECIFICITY, true, 20, "마일스톤 구체적")),
                "AI 검증 통과"
        );
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willReturn(result);
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(trustScoreRepository.save(any(TrustScore.class))).willReturn(savedHighTrustScore);
        given(ideaRepository.findByIdForUpdate(1L)).willReturn(Optional.of(idea));
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(expertMatchRepository.existsByIdeaIdAndStatus(1L, MatchStatus.ACCEPTED)).willReturn(false);

        processor.processAiVerification(new VerificationRequestedEvent(null, request("정상 제목", "정상 설명")));

        assertThat(idea.getBadge()).isEqualTo(IdeaBadge.VERIFIED);
        then(ideaRepository).should().save(idea);
    }

    @Test
    @DisplayName("AI 검증 장애 시 관리자 검토 전환 성공")
    void processAiVerification_AI장애관리자검토성공() {
        ProjectVerification verification = aiVerifyingVerification();
        given(projectVerificationRepository.findById(null)).willReturn(Optional.of(verification));
        given(openAiVerificationService.verify(any())).willThrow(new RuntimeException("AI 장애"));

        processor.processAiVerification(new VerificationRequestedEvent(null, request("정상 제목", "정상 설명")));

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.PENDING_ADMIN_REVIEW);
        then(auditLogRepository).should().save(any(VerificationAuditLog.class));
    }
}