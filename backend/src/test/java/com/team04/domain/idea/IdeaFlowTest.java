package com.team04.domain.idea;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.idea.dto.request.CreateIdeaRequest;
import com.team04.domain.idea.dto.request.CreateMilestoneRequest;
import com.team04.domain.idea.dto.response.IdeaResponse;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.idea.service.IdeaAdminService;
import com.team04.domain.idea.service.IdeaService;
import com.team04.domain.match.dto.request.ExpertMatchRespondRequest;
import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.match.dto.request.MatchRequest;
import com.team04.domain.match.dto.response.ExpertMatchResponse;
import com.team04.domain.match.entity.Feasibility;
import com.team04.domain.match.entity.MatchStatus;
import com.team04.domain.match.service.ExpertMatchService;
import com.team04.domain.match.service.ExpertReviewService;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.verification.dto.request.VerificationRequest;
import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationStatus;
import com.team04.domain.verification.event.VerificationRequestedEvent;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.service.VerificationAsyncProcessor;
import com.team04.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class IdeaFlowTest {

    @Autowired
    private IdeaService ideaService;

    @Autowired
    private ExpertMatchService expertMatchService;

    @Autowired
    private ExpertReviewService expertReviewService;

    @Autowired
    private IdeaAdminService ideaAdminService;

    @Autowired
    private VerificationAsyncProcessor verificationAsyncProcessor;

    @Autowired
    private com.team04.domain.verification.service.VerificationService verificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpertProfileRepository expertProfileRepository;

    @Autowired
    private IdeaRepository ideaRepository;

    @Autowired
    private ProjectVerificationRepository projectVerificationRepository;

    private CreateIdeaRequest savedIdeaRequest;

    private Long proposerId;
    private Long expertUserId;
    private Long expertProfileId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User proposer = userRepository.save(User.create(
                "idea-flow-proposer-" + suffix + "@test.com",
                "password1!",
                "제안자",
                "제안자" + suffix,
                30,
                Role.USER
        ));
        proposerId = proposer.getId();

        User expert = userRepository.save(User.create(
                "idea-flow-expert-" + suffix + "@test.com",
                "password1!",
                "전문가",
                "전문가" + suffix,
                35,
                Role.EXPERT
        ));
        expertUserId = expert.getId();

        ExpertProfile expertProfile = expertProfileRepository.save(ExpertProfile.builder()
                .user(expert)
                .qualificationType(QualificationType.NATIONAL_QUALIFICATION)
                .qualificationNumber("CERT-" + suffix)
                .build());
        expertProfileId = expertProfile.getId();

        savedIdeaRequest = createIdeaRequest("기본");
    }

    @Test
    @DisplayName("아이디어 등록부터 펀딩 오픈까지 전체 흐름이 순서대로 전이된다")
    void 아이디어_등록부터_펀딩_오픈까지_전체_흐름이_순서대로_전이된다() throws Exception {
        IdeaResponse created = ideaService.createIdea(proposerId, savedIdeaRequest);
        Long ideaId = created.ideaId();

        assertIdeaStatus(ideaId, IdeaStatus.AI_PENDING);
        triggerVerification(ideaId);
        assertVerificationStatus(ideaId, VerificationStatus.AI_VERIFYING);

        processAiVerificationSynchronously(ideaId);

        assertIdeaStatus(ideaId, IdeaStatus.EXPERT_PENDING);
        assertVerificationStatus(ideaId, VerificationStatus.AI_PASSED);

        ExpertMatchResponse requested = expertMatchService.requestMatch(
                proposerId,
                expertProfileId,
                new MatchRequest(ideaId)
        );
        ExpertMatchResponse accepted = expertMatchService.respond(
                expertUserId,
                requested.matchId(),
                new ExpertMatchRespondRequest(MatchStatus.ACCEPTED, null)
        );

        assertThat(accepted.status()).isEqualTo(MatchStatus.ACCEPTED);
        assertIdeaStatus(ideaId, IdeaStatus.EXPERT_PENDING);
        assertVerificationStatus(ideaId, VerificationStatus.EXPERT_MATCHING);

        expertReviewService.createReview(
                expertUserId,
                requested.matchId(),
                createExpertReviewRequest()
        );

        assertIdeaStatus(ideaId, IdeaStatus.ADMIN_PENDING);
        assertVerificationStatus(ideaId, VerificationStatus.PENDING_ADMIN_REVIEW);

        ideaAdminService.approve(ideaId);

        assertIdeaStatus(ideaId, IdeaStatus.OPEN);
        assertVerificationStatus(ideaId, VerificationStatus.PENDING_ADMIN_REVIEW);
    }

    @Test
    @DisplayName("중간 단계를 건너뛰고 관리자 승인 시도 시 예외가 발생한다")
    void 중간_단계를_건너뛰고_관리자_승인_시도_시_예외가_발생한다() {
        IdeaResponse created = ideaService.createIdea(proposerId, savedIdeaRequest);

        assertThatThrownBy(() -> ideaAdminService.approve(created.ideaId()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("매칭 수락 전 전문가 검토 제출 시 예외가 발생한다")
    void 매칭_수락_전_전문가_검토_제출_시_예외가_발생한다() throws Exception {
        IdeaResponse created = ideaService.createIdea(proposerId, savedIdeaRequest);
        Long ideaId = created.ideaId();
        triggerVerification(ideaId);
        processAiVerificationSynchronously(ideaId);

        ExpertMatchResponse requested = expertMatchService.requestMatch(
                proposerId,
                expertProfileId,
                new MatchRequest(ideaId)
        );

        assertThatThrownBy(() -> expertReviewService.createReview(
                expertUserId,
                requested.matchId(),
                createExpertReviewRequest()
        )).isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("유효하지 않은 VerificationStatus 전이 시 예외가 발생한다")
    void 유효하지_않은_VerificationStatus_전이_시_예외가_발생한다() {
        IdeaResponse created = ideaService.createIdea(proposerId, savedIdeaRequest);
        triggerVerification(created.ideaId());
        ProjectVerification verification = findVerification(created.ideaId());

        assertThatThrownBy(() -> verification.changeStatus(VerificationStatus.EXPERT_MATCHING))
                .isInstanceOf(CustomException.class);
    }

    private void triggerVerification(Long ideaId) {
        verificationService.requestVerification(createVerificationRequest(ideaId), proposerId);
    }

    private void processAiVerificationSynchronously(Long ideaId) throws Exception {
        VerificationAsyncProcessor target = AopTestUtils.getTargetObject(verificationAsyncProcessor);
        target.processAiVerification(new VerificationRequestedEvent(
                findVerification(ideaId).getId(),
                createVerificationRequest(ideaId)
        ));
    }

    private CreateIdeaRequest createIdeaRequest(String titleSuffix) {
        LocalDateTime fundingStartAt = LocalDateTime.now().plusDays(10);
        return new CreateIdeaRequest(
                "아이디어 " + titleSuffix,
                IdeaCategory.TECH,
                "AI 기반 일정 관리 서비스",
                "소규모 팀은 일정 조율과 업무 우선순위 관리에 많은 시간을 사용합니다.",
                "팀 업무 데이터를 분석해 우선순위와 일정을 추천합니다.",
                "반복적인 일정 조율 시간을 줄이고 실행률을 높입니다.",
                "프로젝트를 운영하는 스타트업과 소규모 팀",
                "기존 협업 도구와 달리 한국형 업무 맥락을 반영합니다.",
                "백엔드, 프론트엔드, AI 개발 경험이 있는 팀입니다.",
                1_000_000L,
                100_000L,
                fundingStartAt,
                fundingStartAt.plusDays(21),
                RewardType.REWARD_POINT,
                null,
                List.of(),
                createMilestones()
        );
    }

    private List<CreateMilestoneRequest> createMilestones() {
        return List.of(
                new CreateMilestoneRequest(
                        1,
                        "요구사항 분석과 화면 설계 완료",
                        "핵심 사용자 시나리오와 와이어프레임 산출",
                        LocalDate.now().plusDays(20)
                ),
                new CreateMilestoneRequest(
                        2,
                        "MVP 기능 개발 완료",
                        "일정 추천 API와 기본 화면 구현",
                        LocalDate.now().plusDays(35)
                ),
                new CreateMilestoneRequest(
                        3,
                        "베타 테스트와 개선 완료",
                        "테스트 피드백 반영 및 출시 후보 버전 준비",
                        LocalDate.now().plusDays(50)
                )
        );
    }

    private VerificationRequest createVerificationRequest(Long ideaId) {
        return new VerificationRequest(
                ideaId,
                savedIdeaRequest.title(),
                String.join("\n",
                        savedIdeaRequest.oneLineIntro(),
                        savedIdeaRequest.problemDefinition(),
                        savedIdeaRequest.solution(),
                        savedIdeaRequest.goal(),
                        savedIdeaRequest.targetCustomer(),
                        savedIdeaRequest.competitor(),
                        savedIdeaRequest.teamIntro()
                ),
                savedIdeaRequest.milestones().stream()
                        .map(milestone -> new VerificationRequest.MilestoneInfo(
                                milestone.goal(),
                                milestone.expectedResult(),
                                milestone.expectedDate(),
                                null
                        ))
                        .toList()
        );
    }

    private ExpertReviewRequest createExpertReviewRequest() {
        return new ExpertReviewRequest(
                Feasibility.POSSIBLE,
                "약 3개월",
                "Java, Spring Boot, React, OpenAI API",
                "외부 API 장애와 추천 품질 튜닝이 주요 리스크입니다.",
                "현재 계획과 팀 역량을 고려하면 MVP 구현 가능성이 높습니다."
        );
    }

    private void assertIdeaStatus(Long ideaId, IdeaStatus expectedStatus) {
        Idea idea = ideaRepository.findByIdAndDeletedAtIsNull(ideaId).orElseThrow();
        assertThat(idea.getStatus()).isEqualTo(expectedStatus);
    }

    private void assertVerificationStatus(Long ideaId, VerificationStatus expectedStatus) {
        assertThat(findVerification(ideaId).getStatus()).isEqualTo(expectedStatus);
    }

    private ProjectVerification findVerification(Long ideaId) {
        return projectVerificationRepository.findByIdeaId(ideaId).orElseThrow();
    }
}