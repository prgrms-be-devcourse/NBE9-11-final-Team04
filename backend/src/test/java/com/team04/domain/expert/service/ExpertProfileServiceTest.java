package com.team04.domain.expert.service;

import com.team04.domain.expert.dto.request.ExpertProfileRequest;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.entity.TechStack;
import com.team04.domain.expert.repository.ExpertAppealRepository;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.AppealStorageClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ExpertProfileServiceTest {

    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExpertAppealRepository expertAppealRepository;
    @Mock private AppealStorageClient appealStorageClient;

    @InjectMocks
    private ExpertProfileService expertProfileService;

    private User user() {
        User user = User.create("expert@test.com", "password", "김전문", "expert_kim", 40, Role.EXPERT);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private ExpertProfile activeProfile() {
        return ExpertProfile.builder()
                .user(user())
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build();
    }

    private ExpertProfileRequest request() {
        return new ExpertProfileRequest(TechStack.TECH, "https://portfolio.url", "10년 경력");
    }

    @Test
    @DisplayName("verified=true인 전문가 프로필 등록 시 techStack, portfolioUrl, career가 업데이트된다")
    void registerProfile_검증완료_프로필업데이트() {
        ExpertProfile profile = activeProfile();
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        var response = expertProfileService.registerProfile(1L, request());

        assertThat(profile.getTechStack()).isEqualTo(TechStack.TECH);
        assertThat(profile.getPortfolioUrl()).isEqualTo("https://portfolio.url");
        assertThat(profile.getCareer()).isEqualTo("10년 경력");
    }

    @Test
    @DisplayName("프로필이 없으면 EXPERT_NOT_FOUND 예외가 발생한다")
    void registerProfile_프로필없음_EXPERT_NOT_FOUND_예외() {
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> expertProfileService.registerProfile(1L, request()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_FOUND);
    }

    @Test
    @DisplayName("verified=false인 프로필 등록 시 EXPERT_NOT_VERIFIED 예외가 발생한다")
    void registerProfile_미검증프로필_EXPERT_NOT_VERIFIED_예외() {
        ExpertProfile profile = ExpertProfile.ofPending(
                user(),
                QualificationType.NATIONAL_QUALIFICATION,
                "Q123456789",
                "https://file.url",
                null, null
        );
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        assertThatThrownBy(() -> expertProfileService.registerProfile(1L, request()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("존재하지 않는 전문가 프로필 조회 시 EXPERT_NOT_FOUND 예외가 발생한다")
    void getProfile_없는프로필_EXPERT_NOT_FOUND_예외() {
        given(expertProfileRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> expertProfileService.getProfile(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 전문가 소명 자료 조회 시 EXPERT_NOT_FOUND 예외가 발생한다")
    void getAppeals_없는프로필_EXPERT_NOT_FOUND_예외() {
        given(expertProfileRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> expertProfileService.getAppeals(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_FOUND);
    }
}