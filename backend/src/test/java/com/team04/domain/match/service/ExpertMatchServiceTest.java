package com.team04.domain.match.service;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.match.dto.request.ExpertMatchRespondRequest;
import com.team04.domain.match.dto.request.MatchRequest;
import com.team04.domain.match.entity.ExpertMatch;
import com.team04.domain.match.entity.MatchStatus;
import com.team04.domain.match.repository.ExpertMatchRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.verification.repository.ProjectVerificationRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ExpertMatchServiceTest {

    @Mock private ExpertMatchRepository expertMatchRepository;
    @Mock private IdeaRepository ideaRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private ProjectVerificationRepository projectVerificationRepository;

    @InjectMocks
    private ExpertMatchService expertMatchService;

    private User user() {
        User user = User.create("user@test.com", "password", "홍길동", "proposer", 35, Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Idea idea(Long userId) {
        return new Idea(
                userId, "AI 농업 플랫폼", IdeaCategory.TECH,
                "한 줄 소개", "문제", "해결", "목표", "고객", "경쟁사", "팀",
                50000000L, 50000000L,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusMonths(1),
                RewardType.REWARD_POINT, null, null
        );
    }

    private Idea adminPendingIdea(Long userId) {
        Idea idea = idea(userId);
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        return idea;
    }

    private ExpertProfile activeProfile() {
        User expert = User.create("expert@test.com", "password", "김전문", "expert", 40, Role.EXPERT);
        ReflectionTestUtils.setField(expert, "id", 2L);
        return ExpertProfile.builder()
                .user(expert)
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build();
    }

    private ExpertMatch pendingMatch(Long ideaId, ExpertProfile profile) {
        ExpertMatch match = ExpertMatch.create(ideaId, profile);
        ReflectionTestUtils.setField(match, "id", 1L);
        return match;
    }

    // ===== requestMatch 테스트 =====

    @Test
    @DisplayName("아이디어 소유자가 아닌 경우 FORBIDDEN 예외가 발생한다")
    void requestMatch_소유자아님_FORBIDDEN_예외() {
        Idea idea = idea(1L);
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));

        assertThatThrownBy(() -> expertMatchService.requestMatch(
                2L, 1L, new MatchRequest(10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("매칭 거절 횟수가 3회 이상이면 MATCH_REQUEST_LIMIT_EXCEEDED 예외가 발생한다")
    void requestMatch_거절횟수초과_MATCH_REQUEST_LIMIT_EXCEEDED_예외() {
        Idea idea = idea(1L);
        idea.increaseRejectedMatchCount();
        idea.increaseRejectedMatchCount();
        idea.increaseRejectedMatchCount();
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));

        assertThatThrownBy(() -> expertMatchService.requestMatch(
                1L, 1L, new MatchRequest(10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.MATCH_REQUEST_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 전문가에게 매칭 요청 시 EXPERT_SUSPENDED 예외가 발생한다")
    void requestMatch_비활성전문가_EXPERT_SUSPENDED_예외() {
        Idea idea = idea(1L);
        ExpertProfile profile = activeProfile();
        profile.suspend();

        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(expertProfileRepository.findById(1L)).willReturn(Optional.of(profile));

        assertThatThrownBy(() -> expertMatchService.requestMatch(
                1L, 1L, new MatchRequest(10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_SUSPENDED);
    }

    @Test
    @DisplayName("동일한 아이디어-전문가 조합으로 중복 매칭 요청 시 MATCH_ALREADY_REQUESTED 예외가 발생한다")
    void requestMatch_중복요청_MATCH_ALREADY_REQUESTED_예외() {
        Idea idea = idea(1L);
        ExpertProfile profile = activeProfile();

        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(expertProfileRepository.findById(1L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.existsByIdeaIdAndExpertProfile_Id(10L, 1L)).willReturn(true);

        assertThatThrownBy(() -> expertMatchService.requestMatch(
                1L, 1L, new MatchRequest(10L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.MATCH_ALREADY_REQUESTED);
    }

    @Test
    @DisplayName("매칭 요청 성공 시 ExpertMatch가 저장된다")
    void requestMatch_성공_매칭저장() {
        Idea idea = idea(1L);
        ExpertProfile profile = activeProfile();

        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));
        given(expertProfileRepository.findById(1L)).willReturn(Optional.of(profile));
        given(expertMatchRepository.existsByIdeaIdAndExpertProfile_Id(10L, 1L)).willReturn(false);
        given(expertMatchRepository.save(any())).willAnswer(i -> i.getArgument(0));

        expertMatchService.requestMatch(1L, 1L, new MatchRequest(10L));

        then(expertMatchRepository).should().save(any(ExpertMatch.class));
    }

    // ===== respond 테스트 =====

    @Test
    @DisplayName("거절 시 거절 사유가 없으면 INVALID_INPUT 예외가 발생한다")
    void respond_거절사유없음_INVALID_INPUT_예외() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = pendingMatch(10L, profile);
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));

        assertThatThrownBy(() -> expertMatchService.respond(
                2L, 1L, new ExpertMatchRespondRequest(MatchStatus.REJECTED, null)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("수락 처리 시 매칭 상태가 ACCEPTED로 변경된다")
    void respond_수락_ACCEPTED_상태변경() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = pendingMatch(10L, profile);
        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));
        given(projectVerificationRepository.findByIdeaId(10L)).willReturn(Optional.empty());

        expertMatchService.respond(2L, 1L, new ExpertMatchRespondRequest(MatchStatus.ACCEPTED, null));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.ACCEPTED);
        assertThat(match.getRespondedAt()).isNotNull();
    }

    @Test
    @DisplayName("거절 처리 시 매칭 상태가 REJECTED로 변경되고 아이디어 거절 횟수가 증가한다")
    void respond_거절_REJECTED_상태변경_거절횟수증가() {
        ExpertProfile profile = activeProfile();
        ExpertMatch match = pendingMatch(10L, profile);
        Idea idea = idea(1L);

        given(expertMatchRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.of(match));
        given(ideaRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(idea));

        expertMatchService.respond(2L, 1L,
                new ExpertMatchRespondRequest(MatchStatus.REJECTED, "검토 후 부적합"));

        assertThat(match.getStatus()).isEqualTo(MatchStatus.REJECTED);
        assertThat(match.getRejectReason()).isEqualTo("검토 후 부적합");
        assertThat(idea.getRejectedMatchCount()).isEqualTo(1);
    }
}