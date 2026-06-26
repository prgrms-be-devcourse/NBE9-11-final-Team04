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

/** VerificationService의 검증 접수와 조회 흐름을 검증하는 테스트입니다. */
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

    /** 테스트용 검증 요청을 생성합니다. */
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

    /** 테스트용 아이디어를 생성합니다. */
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

    /** 신규 검증 요청이 AI_VERIFYING 상태로 저장되고 이벤트를 발행하는지 확인합니다. */
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

    /** 진행 중인 검증 요청에 중복 접수를 차단하는지 확인합니다. */
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

    /** 이미 완료된 검증 요청에 재제출 개념 없이 재접수를 차단하는지 확인합니다. */
    @Test
    @DisplayName("완료된 검증 요청 재접수 시 상태 전이 예외 발생")
    void requestVerification_완료상태예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.completeAiVerification();
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> verificationService.requestVerification(request(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VERIFICATION_STATUS_TRANSITION);
        then(projectVerificationRepository).should(never()).save(any());
    }
}