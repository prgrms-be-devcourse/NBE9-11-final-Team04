package com.team04.domain.expert.service;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
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

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ExpertVerificationServiceTest {

    @Mock
    private ExpertProfileRepository expertProfileRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ExpertVerificationService expertVerificationService;

    private User user() {
        User user = User.create("expert@test.com", "password", "김전문", "expert_kim", 40, Role.EXPERT);
        ReflectionTestUtils.setField(user, "id", 1L); // 추가
        return user;
    }

    private ExpertProfile activeProfile() {
        return ExpertProfile.builder()
                .user(user())
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build();
    }

    private ExpertProfile suspendedProfile() {
        ExpertProfile profile = activeProfile();
        profile.suspend();
        return profile;
    }

    @Test
    @DisplayName("suspendProfile 호출 시 status=SUSPENDED로 변경되고 알림이 발송된다")
    void suspendProfile_호출시_SUSPENDED_알림발송() {
        ExpertProfile profile = activeProfile();
        given(expertProfileRepository.findById(1L)).willReturn(Optional.of(profile));

        expertVerificationService.suspendProfile(1L);

        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.SUSPENDED);
        assertThat(profile.getSuspendedAt()).isNotNull();
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("suspendProfile 호출 시 프로필이 없으면 EXPERT_NOT_FOUND 예외가 발생한다")
    void suspendProfile_프로필없음_EXPERT_NOT_FOUND_예외() {
        given(expertProfileRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> expertVerificationService.suspendProfile(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_FOUND);
    }

    @Test
    @DisplayName("restoreProfile 호출 시 status=ACTIVE로 복구되고 알림이 발송된다")
    void restoreProfile_호출시_ACTIVE_알림발송() {
        ExpertProfile profile = suspendedProfile();
        given(expertProfileRepository.findById(1L)).willReturn(Optional.of(profile));

        expertVerificationService.restoreProfile(1L);

        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.ACTIVE);
        assertThat(profile.getSuspendedAt()).isNull();
        assertThat(profile.getAppealCount()).isZero();
        then(eventPublisher).should().publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("restoreProfile 호출 시 SUSPENDED 상태가 아니면 EXPERT_NOT_SUSPENDED 예외가 발생한다")
    void restoreProfile_SUSPENDED아님_EXPERT_NOT_SUSPENDED_예외() {
        ExpertProfile profile = activeProfile();
        given(expertProfileRepository.findById(1L)).willReturn(Optional.of(profile));

        assertThatThrownBy(() -> expertVerificationService.restoreProfile(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_SUSPENDED);
    }

    @Test
    @DisplayName("demoteToSponsor 호출 시 status=DEMOTED, role=USER로 변경된다")
    void demoteToSponsor_호출시_DEMOTED_USER역할변경() {
        ExpertProfile profile = suspendedProfile();
        given(expertProfileRepository.findById(1L)).willReturn(Optional.of(profile));

        expertVerificationService.demoteToSponsor(1L);

        assertThat(profile.getStatus()).isEqualTo(ExpertStatus.DEMOTED);
        assertThat(profile.getUser().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("demoteToSponsor 호출 시 프로필이 없으면 EXPERT_NOT_FOUND 예외가 발생한다")
    void demoteToSponsor_프로필없음_EXPERT_NOT_FOUND_예외() {
        given(expertProfileRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> expertVerificationService.demoteToSponsor(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_FOUND);
    }
}