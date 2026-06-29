package com.team04.domain.expert.service;

import com.team04.domain.expert.dto.request.ExpertAppealRequest;
import com.team04.domain.expert.entity.ExpertAppeal;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertAppealRepository;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.AppealStorageClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ExpertAppealServiceTest {

    @Mock
    private ExpertProfileRepository expertProfileRepository;
    @Mock private ExpertAppealRepository expertAppealRepository;
    @Mock private AppealStorageClient appealStorageClient;

    @InjectMocks
    private ExpertAppealService expertAppealService;

    private User user() {
        User user = User.create("expert@test.com", "password", "김전문", "expert_kim", 40, Role.EXPERT);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private ExpertProfile suspendedProfile() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user())
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build();
        profile.suspend();
        return profile;
    }

    private ExpertAppealRequest request() {
        return new ExpertAppealRequest("소명 내용입니다.");
    }

    @Test
    @DisplayName("SUSPENDED 상태가 아닌 전문가가 소명 자료 제출 시 EXPERT_NOT_SUSPENDED 예외가 발생한다")
    void submitAppeal_SUSPENDED아님_EXPERT_NOT_SUSPENDED_예외() {
        ExpertProfile profile = ExpertProfile.builder()
                .user(user())
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build(); // ACTIVE 상태

        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        assertThatThrownBy(() -> expertAppealService.submitAppeal(1L, request(), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_SUSPENDED);

        then(expertAppealRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("소명 기간 7일이 초과된 경우 APPEAL_PERIOD_EXPIRED 예외가 발생한다")
    void submitAppeal_소명기간초과_APPEAL_PERIOD_EXPIRED_예외() {
        ExpertProfile profile = suspendedProfile();
        ReflectionTestUtils.setField(profile, "suspendedAt", LocalDateTime.now().minusDays(8));

        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        assertThatThrownBy(() -> expertAppealService.submitAppeal(1L, request(), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPEAL_PERIOD_EXPIRED);

        then(expertAppealRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("소명 자료 제출 횟수가 3회 이상이면 APPEAL_LIMIT_EXCEEDED 예외가 발생한다")
    void submitAppeal_제출횟수초과_APPEAL_LIMIT_EXCEEDED_예외() {
        ExpertProfile profile = suspendedProfile();
        profile.increaseAppealCount();
        profile.increaseAppealCount();
        profile.increaseAppealCount();

        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        assertThatThrownBy(() -> expertAppealService.submitAppeal(1L, request(), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.APPEAL_LIMIT_EXCEEDED);

        then(expertAppealRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("파일 없이 소명 자료 제출 시 fileKey=null로 저장되고 appealCount가 증가한다")
    void submitAppeal_파일없음_정상저장() {
        ExpertProfile profile = suspendedProfile();
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
        given(expertAppealRepository.save(any())).willAnswer(i -> i.getArgument(0));

        expertAppealService.submitAppeal(1L, request(), null);

        then(appealStorageClient).should(never()).upload(any());
        then(expertAppealRepository).should().save(any(ExpertAppeal.class));
        assertThat(profile.getAppealCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("파일과 함께 소명 자료 제출 시 파일이 업로드되고 appealCount가 증가한다")
    void submitAppeal_파일있음_정상저장() {
        ExpertProfile profile = suspendedProfile();
        MultipartFile file = new MockMultipartFile(
                "file", "appeal.pdf", "application/pdf", "content".getBytes()
        );

        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(profile));
        given(appealStorageClient.upload(file)).willReturn("expert/appeal/uuid.pdf");
        given(expertAppealRepository.save(any())).willAnswer(i -> i.getArgument(0));

        expertAppealService.submitAppeal(1L, request(), file);

        then(appealStorageClient).should().upload(file);
        then(expertAppealRepository).should().save(any(ExpertAppeal.class));
        assertThat(profile.getAppealCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("프로필이 없으면 EXPERT_NOT_FOUND 예외가 발생한다")
    void submitAppeal_프로필없음_EXPERT_NOT_FOUND_예외() {
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> expertAppealService.submitAppeal(1L, request(), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_FOUND);
    }
}