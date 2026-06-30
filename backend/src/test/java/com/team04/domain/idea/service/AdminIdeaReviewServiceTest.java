package com.team04.domain.idea.service;

import com.team04.domain.idea.entity.Idea;
import com.team04.domain.idea.entity.IdeaCategory;
import com.team04.domain.idea.entity.IdeaStatus;
import com.team04.domain.idea.entity.RewardType;
import com.team04.domain.idea.repository.IdeaRepository;
import com.team04.domain.verification.entity.TrustScore;
import com.team04.domain.verification.repository.TrustScoreRepository;
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
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminIdeaReviewServiceTest {

    @Mock private IdeaAdminService ideaAdminService;
    @Mock private IdeaRepository ideaRepository;
    @Mock private TrustScoreRepository trustScoreRepository;
    @Mock private IdeaService ideaService;

    @InjectMocks
    private AdminIdeaReviewService adminIdeaReviewService;

    private Idea adminPendingIdea(int adminRejectedCount) {
        Idea idea = new Idea(
                1L, "AI 농업 플랫폼", IdeaCategory.TECH,
                "한 줄 소개", "문제", "해결", "목표", "고객", "경쟁사", "팀",
                50000000L, 50000000L,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 9, 15, 0, 0),
                RewardType.REWARD_POINT, null, null
        );
        ReflectionTestUtils.setField(idea, "id", 1L);
        idea.changeStatus(IdeaStatus.EXPERT_PENDING);
        idea.changeStatus(IdeaStatus.ADMIN_PENDING);
        for (int i = 0; i < adminRejectedCount; i++) {
            idea.increaseAdminRejectedCount();
        }
        return idea;
    }

    // ===== approve 테스트 =====

    @Test
    @DisplayName("반려 0회 승인 시 adminApprovalScore가 20점으로 업데이트된다")
    void approve_반려0회_adminApprovalScore_20점() {
        Idea idea = adminPendingIdea(0);
        TrustScore trustScore = new TrustScore(1L);

        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.of(trustScore));

        adminIdeaReviewService.approve(1L);

        assertThat(trustScore.getAdminApprovalScore()).isEqualTo(20);
        then(trustScoreRepository).should().save(trustScore);
        then(ideaAdminService).should().approve(1L);
    }

    @Test
    @DisplayName("반려 1회 승인 시 adminApprovalScore가 10점으로 업데이트된다")
    void approve_반려1회_adminApprovalScore_10점() {
        Idea idea = adminPendingIdea(1);
        TrustScore trustScore = new TrustScore(1L);

        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.of(trustScore));

        adminIdeaReviewService.approve(1L);

        assertThat(trustScore.getAdminApprovalScore()).isEqualTo(10);
    }

    @Test
    @DisplayName("반려 2회 이상 승인 시 adminApprovalScore가 5점으로 업데이트된다")
    void approve_반려2회이상_adminApprovalScore_5점() {
        Idea idea = adminPendingIdea(2);
        TrustScore trustScore = new TrustScore(1L);

        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.of(trustScore));

        adminIdeaReviewService.approve(1L);

        assertThat(trustScore.getAdminApprovalScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("TrustScore가 없으면 점수 업데이트 없이 승인만 처리된다")
    void approve_TrustScore없음_승인만처리() {
        Idea idea = adminPendingIdea(0);

        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(idea));
        given(trustScoreRepository.findByIdeaId(1L)).willReturn(Optional.empty());

        adminIdeaReviewService.approve(1L);

        then(trustScoreRepository).should(never()).save(any());
        then(ideaAdminService).should().approve(1L);
    }

    @Test
    @DisplayName("아이디어가 없으면 IDEA_NOT_FOUND 예외가 발생한다")
    void approve_아이디어없음_IDEA_NOT_FOUND_예외() {
        given(ideaRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminIdeaReviewService.approve(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDEA_NOT_FOUND);

        then(ideaAdminService).should(never()).approve(any());
    }

    // ===== reject 테스트 =====

    @Test
    @DisplayName("거절 시 IdeaAdminService.reject()가 호출된다")
    void reject_IdeaAdminService_reject_호출() {
        adminIdeaReviewService.reject(1L, "보완이 필요합니다.");

        then(ideaAdminService).should().reject(1L, "보완이 필요합니다.");
    }
}