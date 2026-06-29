package com.team04.domain.match.service;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.dto.request.ExpertReviewRequest;
import com.team04.domain.match.entity.ExpertMatch;
import com.team04.domain.match.entity.Feasibility;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.match.repository.ExpertReviewRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.verification.entity.TrustScore;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.domain.verification.repository.TrustScoreRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ExpertReviewServiceTest {

    @Mock private ExpertMatchRepository expertMatchRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private ExpertReviewRepository expertReviewRepository;
    @Mock private IdeaRepository ideaRepository;
    @Mock private TrustScoreRepository trustScoreRepository;
    @Mock private ProjectVerificationRepository projectVerificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ExpertReviewService expertReviewService;

    private User expertUser() {
        User user = User.create("expert@test.com", "password", "김전문", "expert", 40, Role.EXPERT);
        ReflectionTestUtils.setField(user, "id", 2L);
        return user;
    }

    private User proposerUser() {
        User user = User.create("user@test.com", "password", "홍길동", "proposer", 35, Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private ExpertProfile activeProfile() {
        return ExpertProfile.builder()
                .user(expertUser())
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build();
    }

    private Idea expertPendingIdea() {
        Idea idea = new Idea(
                1L, "AI 농업 플랫폼", IdeaCategory.TECH,
                "한 줄 소개", "문제", "해결", "목표", "고객", "경쟁사", "팀",
                50000000L, 50000000L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusMonths(1),
                RewardType.REWARD_POINT, null, null
        );
        ReflectionTestUtils.setField(idea, "id", 10L);      // 추가 — idea.getId() = 10L
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        return idea;
    }

    private ExpertMatch acceptedMatch(Long ideaId, ExpertProfile profile) {
        ExpertMatch match = ExpertMatch.create(ideaId, profile);
        ReflectionTestUtils.setField(match, "id", 1L);
        match.accept();
        return match;
    }

    private ExpertReviewRequest reviewRequest(Feasibility feasibility) {
        return new ExpertReviewRequest(
                feasibility, "3개월", "Spring Boot, React", "일정 지연 가능성", "구현 가능합니다."
        );
    }

    @Test
    @DisplayName("전문가 프로필이 없으면 EXPERT_NOT_FOUND 예외가 발생한다")
    void createReview_프로필없음_EXPERT_NOT_FOUND_예외() {
        given(expertProfileRepository.findByUserId(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> expertReviewService.createReview(
                2L, 1L, reviewRequest(Feasibility.POSSIBLE)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_FOUND);
    }

    @Test
    @DisplayName("수락되지 않은 매칭에 검토서 작성 시 MATCH_NOT_ACCEPTED 예외가 발생한다")
    void createReview_미수락매칭_MATCH_NOT_ACCEPTED_예외() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = ExpertMatch.create(10L, profile);
        ReflectionTestUtils.setField(match, "id", 1L);

        given(expertProfileRepository.findByUserId(2L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));

        assertThatThrownBy(() -> expertReviewService.createReview(
                2L, 1L, reviewRequest(Feasibility.POSSIBLE)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.MATCH_NOT_ACCEPTED);
    }

    @Test
    @DisplayName("이미 검토서가 작성된 매칭에 재작성 시 REVIEW_ALREADY_EXISTS 예외가 발생한다")
    void createReview_중복검토서_REVIEW_ALREADY_EXISTS_예외() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = acceptedMatch(10L, profile);

        given(expertProfileRepository.findByUserId(2L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));
        given(expertReviewRepository.existsByExpertMatch_Id(1L)).willReturn(true);

        assertThatThrownBy(() -> expertReviewService.createReview(
                2L, 1L, reviewRequest(Feasibility.POSSIBLE)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("POSSIBLE 검토서 작성 시 expertMatchingScore가 20점으로 업데이트된다")
    void createReview_POSSIBLE_trustScore_20점() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = acceptedMatch(10L, profile);
        Idea idea = expertPendingIdea();
        TrustScore trustScore = new TrustScore(10L);

        given(expertProfileRepository.findByUserId(2L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));
        given(expertReviewRepository.existsByExpertMatch_Id(1L)).willReturn(false);
        given(expertReviewRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(10L)).willReturn(Optional.of(trustScore));
        given(projectVerificationRepository.findByIdeaId(10L)).willReturn(Optional.empty());

        expertReviewService.createReview(2L, 1L, reviewRequest(Feasibility.POSSIBLE));

        assertThat(trustScore.getExpertMatchingScore()).isEqualTo(20);
        then(trustScoreRepository).should().save(trustScore);
    }

    @Test
    @DisplayName("IMPOSSIBLE 검토서 작성 시 expertMatchingScore가 10점으로 업데이트된다")
    void createReview_IMPOSSIBLE_trustScore_10점() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = acceptedMatch(10L, profile);
        Idea idea = expertPendingIdea();
        TrustScore trustScore = new TrustScore(10L);

        given(expertProfileRepository.findByUserId(2L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));
        given(expertReviewRepository.existsByExpertMatch_Id(1L)).willReturn(false);
        given(expertReviewRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(10L)).willReturn(Optional.of(trustScore));
        given(projectVerificationRepository.findByIdeaId(10L)).willReturn(Optional.empty());

        expertReviewService.createReview(2L, 1L, reviewRequest(Feasibility.IMPOSSIBLE));

        assertThat(trustScore.getExpertMatchingScore()).isEqualTo(10);
    }

    @Test
    @DisplayName("검토서 작성 시 EXPERT_PENDING 상태 아이디어가 ADMIN_PENDING으로 전환된다")
    void createReview_아이디어상태_ADMIN_PENDING_전환() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = acceptedMatch(10L, profile);
        Idea idea = expertPendingIdea();
        TrustScore trustScore = new TrustScore(10L);

        given(expertProfileRepository.findByUserId(2L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));
        given(expertReviewRepository.existsByExpertMatch_Id(1L)).willReturn(false);
        given(expertReviewRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(10L)).willReturn(Optional.of(trustScore));
        given(projectVerificationRepository.findByIdeaId(10L)).willReturn(Optional.empty());

        expertReviewService.createReview(2L, 1L, reviewRequest(Feasibility.POSSIBLE));

        assertThat(idea.getStatus()).isEqualTo(IdeaStatus.ADMIN_PENDING);
    }

    @Test
    @DisplayName("검토서 작성 성공 시 제안자에게 알림이 발송된다")
    void createReview_성공_알림발송() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = acceptedMatch(10L, profile);
        Idea idea = expertPendingIdea();
        TrustScore trustScore = new TrustScore(10L);

        given(expertProfileRepository.findByUserId(2L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));
        given(expertReviewRepository.existsByExpertMatch_Id(1L)).willReturn(false);
        given(expertReviewRepository.save(any())).willAnswer(i -> i.getArgument(0));
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(10L)).willReturn(Optional.of(trustScore));
        given(projectVerificationRepository.findByIdeaId(10L)).willReturn(Optional.empty());

        expertReviewService.createReview(2L, 1L, reviewRequest(Feasibility.POSSIBLE));

        then(eventPublisher).should().publishEvent(any(Object.class));
    }
}