package com.team04.domain.verification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationAuditLog;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.event.VerificationRequestedEvent;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.repository.VerificationAuditLogRepository;
import com.team04.domain.verification.repository.VerificationResultRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private ProjectVerificationRepository projectVerificationRepository;
    @Mock
    private VerificationAuditLogRepository auditLogRepository;
    @Mock
    private VerificationResultRepository verificationResultRepository;
    @Mock
    private IdeaRepository ideaRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VerificationService verificationService;

    private VerificationRequest request() {
        return new VerificationRequest(
                1L,
                "검증 요청 제목",
                "검증 요청 설명",
                List.of(new VerificationRequest.MilestoneInfo(
                        "목표",
                        "결과물",
                        LocalDate.now().plusMonths(1),
                        10000L
                ))
        );
    }

    private Idea idea(Long userId) {
        return new Idea(
                userId,
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
    @DisplayName("신규 검증 요청 성공")
    void requestVerification_신규요청성공() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.empty());
        given(projectVerificationRepository.save(any(ProjectVerification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = verificationService.requestVerification(request(), 1L);

        assertThat(response.status()).isEqualTo(VerificationStatus.AI_VERIFYING);
        then(auditLogRepository).should().save(any(VerificationAuditLog.class));
        then(eventPublisher).should().publishEvent(any(VerificationRequestedEvent.class));
    }

    @Test
    @DisplayName("보완 필요 상태 검증 요청 시 재제출 API 사용 예외 발생")
    void requestVerification_보완상태예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.requestRevision(LocalDateTime.now().plusDays(7));
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> verificationService.requestVerification(request(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USE_RESUBMIT_API);

        then(projectVerificationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("진행 중인 검증 요청 시 중복 진행 예외 발생")
    void requestVerification_진행중예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> verificationService.requestVerification(request(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.VERIFICATION_ALREADY_IN_PROGRESS);
    }

    @Test
    @DisplayName("보완안 재제출 성공")
    void resubmit_성공() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.requestRevision(LocalDateTime.now().plusDays(7));
        given(projectVerificationRepository.findById(1L)).willReturn(Optional.of(verification));

        var response = verificationService.resubmit(1L, 1L, request());

        assertThat(response.status()).isEqualTo(VerificationStatus.AI_VERIFYING);
        assertThat(response.resubmissionCount()).isEqualTo(1);
        then(auditLogRepository).should().save(any(VerificationAuditLog.class));
        then(eventPublisher).should().publishEvent(any(VerificationRequestedEvent.class));
    }

    @Test
    @DisplayName("대기 기간 중 재제출 시 예외 발생")
    void resubmit_대기기간예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.updateWaitingUntil(LocalDateTime.now().plusDays(1));
        given(projectVerificationRepository.findById(1L)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> verificationService.resubmit(1L, 1L, request()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.VERIFICATION_WAITING_PERIOD_ACTIVE);
    }

    @Test
    @DisplayName("보완 기한 만료 검증 건 자동 반려 성공")
    void rejectExpiredRevisionRequests_성공() {
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.requestRevision(LocalDateTime.now().minusDays(1));
        given(projectVerificationRepository.findAllByStatusAndRevisionDueAtBefore(
                any(VerificationStatus.class),
                any(LocalDateTime.class)
        )).willReturn(List.of(verification));
        given(ideaRepository.findByIdInAndDeletedAtIsNull(any())).willReturn(List.of(idea(1L)));
        ArgumentCaptor<VerificationAuditLog> captor = ArgumentCaptor.forClass(VerificationAuditLog.class);

        verificationService.rejectExpiredRevisionRequests();

        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        then(auditLogRepository).should().save(captor.capture());
        assertThat(captor.getValue().getNextStatus()).isEqualTo(VerificationStatus.REJECTED);
    }

    @Test
    @DisplayName("재제출 3회 초과 시 30일 대기 반려 처리")
    void resubmit_3회초과대기반려() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.requestRevision(LocalDateTime.now().plusDays(7));
        verification.resubmit();
        verification.requestRevision(LocalDateTime.now().plusDays(7));
        verification.resubmit();
        verification.requestRevision(LocalDateTime.now().plusDays(7));
        verification.resubmit();
        given(projectVerificationRepository.findById(1L)).willReturn(Optional.of(verification));

        var response = verificationService.resubmit(1L, 1L, request());

        assertThat(response.status()).isEqualTo(VerificationStatus.REJECTED);
        assertThat(verification.getWaitingUntil()).isAfter(LocalDateTime.now());
        then(auditLogRepository).should().save(any(VerificationAuditLog.class));
        then(eventPublisher).should(never()).publishEvent(any(VerificationRequestedEvent.class));
    }
}