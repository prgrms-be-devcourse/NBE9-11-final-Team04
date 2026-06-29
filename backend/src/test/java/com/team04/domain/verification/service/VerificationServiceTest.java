package com.team04.domain.verification.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.user.entity.Role;
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
import static org.mockito.Mockito.verifyNoInteractions;

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
    @Mock
    private ExpertMatchRepository expertMatchRepository;

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

    /** 완료된 검증은 재심사 요청이 가능합니다. */
    @Test
    @DisplayName("완료된 검증 요청 재접수 성공")
    void requestVerification_완료상태재접수성공() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.completeAiVerification();
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));
        given(projectVerificationRepository.save(any(ProjectVerification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = verificationService.requestVerification(request(), 1L);

        assertThat(response.status()).isEqualTo(VerificationStatus.AI_VERIFYING);
        then(eventPublisher).should().publishEvent(any(VerificationRequestedEvent.class));
    }

    /** 아이디어 제안자가 아닌 사용자는 검증을 요청할 수 없습니다. */
    @Test
    @DisplayName("작성자가 아닌 사용자의 검증 요청 시 권한 예외 발생")
    void requestVerification_작성자아님예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));

        assertThatThrownBy(() -> verificationService.requestVerification(request(), 2L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        then(projectVerificationRepository).should(never()).save(any(ProjectVerification.class));
        verifyNoInteractions(eventPublisher);
    }

    /** 취소된 검증은 재심사 요청 상태로 되돌릴 수 없습니다. */
    @Test
    @DisplayName("취소된 검증 요청 재접수 시 상태 전이 예외 발생")
    void requestVerification_취소상태재접수예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        ProjectVerification verification = new ProjectVerification(1L);
        verification.startAiVerification();
        verification.changeStatus(VerificationStatus.CANCELLED);
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> verificationService.requestVerification(request(), 1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VERIFICATION_STATUS_TRANSITION);

        verifyNoInteractions(eventPublisher);
    }

    /** 일반 사용자는 본인 아이디어의 검증 결과만 조회할 수 있습니다. */
    @Test
    @DisplayName("일반 사용자가 타인 아이디어 검증 조회 시 권한 예외 발생")
    void getVerificationByIdeaId_일반사용자타인아이디어권한예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));

        assertThatThrownBy(() -> verificationService.getVerificationByIdeaId(1L, 2L, Role.USER))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        then(projectVerificationRepository).should(never()).findByIdeaId(1L);
    }

    /** 관리자는 소유자 검증 없이 검증 결과를 조회할 수 있습니다. */
    @Test
    @DisplayName("관리자 검증 결과 조회 성공")
    void getVerificationByIdeaId_관리자조회성공() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L))); // 추가
        ProjectVerification verification = new ProjectVerification(1L);
        given(projectVerificationRepository.findByIdeaId(1L)).willReturn(Optional.of(verification));
        given(verificationResultRepository.findAllByIdeaId(1L)).willReturn(List.of());

        var response = verificationService.getVerificationByIdeaId(1L, 99L, Role.ADMIN);

        assertThat(response.status()).isEqualTo(VerificationStatus.DRAFT);
    }

    @Test
    @DisplayName("매칭되지 않은 전문가가 비공개 아이디어 검증 조회 시 권한 예외 발생")
    void getVerificationByIdeaId_비매칭전문가비공개아이디어권한예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea(1L)));
        given(expertMatchRepository.existsByIdeaIdAndUserId(1L, 2L)).willReturn(false);

        assertThatThrownBy(() -> verificationService.getVerificationByIdeaId(1L, 2L, Role.EXPERT))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);

        then(projectVerificationRepository).should(never()).findByIdeaId(1L);
    }
}