package com.team04.domain.idea.service;

import com.team04.domain.idea.dto.request.CreateIdeaRequest;
import com.team04.domain.idea.dto.request.CreateMilestoneRequest;
import com.team04.domain.idea.dto.request.ReportIdeaRequest;
import com.team04.domain.idea.dto.request.UpdateIdeaRequest;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.match.repository.ExpertReviewRepository;
import com.team04.domain.milestone.repository.MilestoneRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.service.VerificationService;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IdeaServiceTest {

    @Mock private IdeaRepository ideaRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private VerificationService verificationService;
    @Mock private ProjectVerificationRepository projectVerificationRepository;
    @Mock private ExpertMatchRepository expertMatchRepository;
    @Mock private ExpertReviewRepository expertReviewRepository;

    @InjectMocks
    private IdeaService ideaService;

    @Test
    @DisplayName("아이디어 생성 시 보증금이 목표 금액의 30퍼센트를 초과하면 INVALID_DEPOSIT_AMOUNT 예외가 발생한다")
    void 아이디어생성_보증금비율초과_INVALID_DEPOSIT_AMOUNT_예외() {
        CreateIdeaRequest request = createRequest(1_000_000L, 300_001L, milestones());

        assertThatThrownBy(() -> ideaService.createIdea(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOSIT_AMOUNT);

        then(ideaRepository).should(never()).save(any(Idea.class));
        verifyNoInteractions(verificationService);
    }

    @Test
    @DisplayName("아이디어 생성 시 마일스톤 단계가 1 2 3이 아니면 INVALID_MILESTONE_STEP 예외가 발생한다")
    void 아이디어생성_마일스톤단계불일치_INVALID_MILESTONE_STEP_예외() {
        CreateIdeaRequest request = createRequest(
                1_000_000L,
                300_000L,
                List.of(milestone(1), milestone(2), milestone(2))
        );

        assertThatThrownBy(() -> ideaService.createIdea(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_MILESTONE_STEP);

        verifyNoInteractions(ideaRepository);
    }

    @Test
    @DisplayName("아이디어 생성 성공 시 마일스톤을 저장하고 AI 검증을 요청한다")
    void 아이디어생성_성공_마일스톤저장_AI검증요청() {
        CreateIdeaRequest request = createRequest(1_000_000L, 300_000L, milestones());
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> {
            Idea savedIdea = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedIdea, "id", 10L);
            return savedIdea;
        });
        ArgumentCaptor<Idea> ideaCaptor = ArgumentCaptor.forClass(Idea.class);
        ArgumentCaptor<VerificationRequest> verificationRequestCaptor =
                ArgumentCaptor.forClass(VerificationRequest.class);

        ideaService.createIdea(1L, request);

        then(ideaRepository).should().save(ideaCaptor.capture());
        then(milestoneRepository).should().saveAll(anyList());
        then(verificationService).should().requestVerification(verificationRequestCaptor.capture(), eq(1L));

        Idea savedIdea = ideaCaptor.getValue();
        VerificationRequest verificationRequest = verificationRequestCaptor.getValue();
        assertThat(savedIdea.getStatus()).isEqualTo(IdeaStatus.AI_PENDING);
        assertThat(savedIdea.getId()).isEqualTo(10L);
        assertThat(verificationRequest.ideaId()).isEqualTo(10L);
        assertThat(verificationRequest.title()).isEqualTo(request.title());
        assertThat(verificationRequest.description())
                .contains(
                        request.oneLineIntro(),
                        request.problemDefinition(),
                        request.solution(),
                        request.goal(),
                        request.targetCustomer(),
                        request.competitor(),
                        request.teamIntro()
                );
        assertThat(verificationRequest.milestones()).hasSize(3);
        assertThat(verificationRequest.milestones().get(0).goal()).isEqualTo("목표 1");
    }

    @Test
    @DisplayName("아이디어 생성 시 보증금이 목표 금액의 30퍼센트와 같으면 예외 없이 통과한다")
    void 아이디어생성_보증금30퍼센트경계값_성공() {
        CreateIdeaRequest request = createRequest(1_000_000L, 300_000L, milestones());
        given(ideaRepository.save(any(Idea.class))).willAnswer(invocation -> {
            Idea savedIdea = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedIdea, "id", 10L);
            return savedIdea;
        });

        ideaService.createIdea(1L, request);

        then(ideaRepository).should().save(any(Idea.class));
        then(verificationService).should().requestVerification(any(), eq(1L));
    }

    @Test
    @DisplayName("진행 중인 본인 아이디어는 취소 신청 상태로 전이되고 검증도 취소된다")
    void 취소신청_본인진행중아이디어_상태전이및검증취소() {
        Idea idea = inProgressIdea(1L);
        ProjectVerification verification = new ProjectVerification(10L);
        verification.startAiVerification();
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(projectVerificationRepository.findByIdeaId(10L)).willReturn(Optional.of(verification));

        ideaService.requestCancellation(10L, 1L);

        assertThat(idea.getStatus()).isEqualTo(IdeaStatus.CANCELLATION_REQUESTED);
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.CANCELLED);
    }

    @Test
    @DisplayName("진행 중인 아이디어라도 작성자가 아니면 FORBIDDEN 예외가 발생한다")
    void 취소신청_작성자아님_FORBIDDEN_예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(inProgressIdea(1L)));

        assertThatThrownBy(() -> ideaService.requestCancellation(10L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("진행 중이 아닌 아이디어에 취소 신청하면 INVALID_IDEA_STATUS_TRANSITION 예외가 발생한다")
    void 취소신청_취소가능상태아님_INVALID_IDEA_STATUS_TRANSITION_예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea(1L)));

        assertThatThrownBy(() -> ideaService.requestCancellation(10L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_IDEA_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("공개 전 아이디어는 일반 사용자가 조회하면 FORBIDDEN 예외가 발생한다")
    void 상세조회_공개전일반사용자_FORBIDDEN_예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea(1L)));

        assertThatThrownBy(() -> ideaService.getIdea(10L, 2L, Role.USER))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("공개 전 아이디어라도 작성자 본인은 조회할 수 있다")
    void 상세조회_공개전작성자본인_조회성공() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea(1L)));
        given(milestoneRepository.findByIdeaIdOrderByStep(10L)).willReturn(List.of());

        ideaService.getIdea(10L, 1L, Role.USER);

        then(milestoneRepository).should().findByIdeaIdOrderByStep(10L);
    }

    @Test
    @DisplayName("공개 전 아이디어라도 매칭된 전문가는 조회할 수 있다")
    void 상세조회_공개전매칭전문가_조회성공() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea(1L)));
        given(expertMatchRepository.existsByIdeaIdAndUserId(10L, 2L)).willReturn(true);
        given(milestoneRepository.findByIdeaIdOrderByStep(10L)).willReturn(List.of());

        ideaService.getIdea(10L, 2L, Role.EXPERT);

        then(milestoneRepository).should().findByIdeaIdOrderByStep(10L);
    }

    @Test
    @DisplayName("자기 아이디어 신고 시 SELF_REPORT_NOT_ALLOWED 예외가 발생한다")
    void 아이디어신고_본인신고_SELF_REPORT_NOT_ALLOWED_예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea(1L)));

        assertThatThrownBy(() -> ideaService.reportIdea(10L, 1L, new ReportIdeaRequest("도용 의심")))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SELF_REPORT_NOT_ALLOWED);
    }

    @Test
    @DisplayName("아이디어 수정은 초기 심사 대기 상태에서 성공한다")
    void 아이디어수정_초기심사대기상태_성공() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea(1L)));

        ideaService.updateIdea(10L, 1L, updateRequest());

        then(milestoneRepository).should().deleteByIdeaIdBulk(10L);
        then(milestoneRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("아이디어 수정은 반려 상태에서 성공하고 AI 재검증을 요청한다")
    void 아이디어수정_반려상태_성공_AI재검증요청() {
        Idea idea = rejectedIdea(1L, 10L);
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(milestoneRepository.findByIdeaIdOrderByStep(10L)).willReturn(milestones().stream()
                .map(m -> com.team04.domain.milestone.entity.Milestone.builder()
                        .ideaId(10L)
                        .step(m.step())
                        .goal(m.goal())
                        .expectedResult(m.expectedResult())
                        .expectedDate(m.expectedDate())
                        .build())
                .toList());
        ArgumentCaptor<VerificationRequest> verificationRequestCaptor = ArgumentCaptor.forClass(VerificationRequest.class);

        ideaService.updateIdea(10L, 1L, updateRequest());

        assertThat(idea.getStatus()).isEqualTo(IdeaStatus.AI_PENDING);
        then(verificationService).should().requestVerification(verificationRequestCaptor.capture(), eq(1L));

        VerificationRequest capturedRequest = verificationRequestCaptor.getValue();
        assertThat(capturedRequest.ideaId()).isEqualTo(10L);
        assertThat(capturedRequest.milestones()).hasSize(3);
        assertThat(capturedRequest.milestones().get(0).goal()).isEqualTo("목표 1");
        assertThat(capturedRequest.milestones().get(1).goal()).isEqualTo("목표 2");
        assertThat(capturedRequest.milestones().get(2).goal()).isEqualTo("목표 3");
    }

    @Test
    @DisplayName("아이디어 수정 가능 상태가 아니면 IDEA_STATUS_NOT_EDITABLE 예외가 발생한다")
    void 아이디어수정_수정가능상태아님_IDEA_STATUS_NOT_EDITABLE_예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(openIdea(1L)));

        assertThatThrownBy(() -> ideaService.updateIdea(10L, 1L, updateRequest()))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.IDEA_STATUS_NOT_EDITABLE);

        then(milestoneRepository).should(never()).deleteByIdeaIdBulk(10L);
        verifyNoInteractions(verificationService);
    }

    private CreateIdeaRequest createRequest(
            Long goalAmount,
            Long depositAmount,
            List<CreateMilestoneRequest> milestones
    ) {
        return new CreateIdeaRequest(
                "아이디어", IdeaCategory.TECH, "한 줄 소개", "문제 정의", "해결책", "목표", "고객",
                "경쟁사", "팀 소개", goalAmount, depositAmount, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(21), RewardType.REWARD_POINT, null, List.of(), milestones
        );
    }

    private UpdateIdeaRequest updateRequest() {
        return new UpdateIdeaRequest(
                "수정 아이디어",
                IdeaCategory.TECH,
                "수정 한 줄 소개",
                "수정 문제 정의",
                "수정 해결책",
                "수정 목표",
                "수정 고객",
                "수정 경쟁사",
                "수정 팀 소개",
                1_000_000L,
                300_000L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(21),
                RewardType.REWARD_POINT,
                null,
                List.of(),
                milestones()
        );
    }

    private List<CreateMilestoneRequest> milestones() {
        return List.of(milestone(1), milestone(2), milestone(3));
    }

    private CreateMilestoneRequest milestone(int step) {
        return new CreateMilestoneRequest(step, "목표 " + step, "결과물 " + step, LocalDate.now().plusMonths(step));
    }

    private Idea inProgressIdea(Long userId) {
        Idea idea = openIdea(userId);
        idea.changeStatus(IdeaStatus.IN_PROGRESS);
        return idea;
    }

    private Idea openIdea(Long userId) {
        Idea idea = idea(userId);
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        idea.changeStatus(IdeaStatus.OPEN);
        return idea;
    }

    private Idea rejectedIdea(Long userId, Long ideaId) {
        Idea idea = idea(userId);
        ReflectionTestUtils.setField(idea, "id", ideaId);
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        idea.reject("보완 필요");
        return idea;
    }

    private Idea idea(Long userId) {
        return new Idea(
                userId, "아이디어", IdeaCategory.TECH, "한 줄 소개", "문제 정의", "해결책", "목표", "고객",
                "경쟁사", "팀 소개", 1_000_000L, 300_000L, LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(21), RewardType.REWARD_POINT, null, null
        );
    }
}