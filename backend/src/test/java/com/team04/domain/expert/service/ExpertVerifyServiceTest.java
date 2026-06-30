package com.team04.domain.expert.service;

import com.team04.domain.expert.client.ExternalVerifyClient;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ExpertVerifyServiceTest {

    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExternalVerifyClient externalVerifyClient;
    @Mock private AppealStorageClient appealStorageClient;

    @InjectMocks
    private ExpertVerifyService expertVerifyService;

    private User user() {
        return User.create("expert@test.com", "password", "김전문", "expert_kim", 40, Role.EXPERT);
    }

    private ExpertVerifyRequest businessRequest() {
        return new ExpertVerifyRequest(
                QualificationType.BUSINESS_REGISTRATION,
                "1234567890",
                null,
                null
        );
    }

    private ExpertVerifyRequest nationalRequest() {
        return new ExpertVerifyRequest(
                QualificationType.NATIONAL_QUALIFICATION,
                "Q123456789",
                null,
                null
        );
    }

    private ExpertVerifyRequest nationalRequestWithoutFile() {
        return new ExpertVerifyRequest(
                QualificationType.NATIONAL_QUALIFICATION,
                "Q123456789",
                null,
                null
        );
    }

    private MultipartFile mockFile() {
        MultipartFile file = mock(MultipartFile.class);
        given(file.isEmpty()).willReturn(false);
        return file;
    }

    @Test
    @DisplayName("이미 verified=true인 프로필이 존재하면 DUPLICATE_EXPERT_PROFILE 예외가 발생한다")
    void verify_이미검증된프로필존재_DUPLICATE_EXPERT_PROFILE_예외() {
        User user = user();
        ExpertProfile existing = ExpertProfile.builder()
                .user(user)
                .qualificationType(QualificationType.BUSINESS_REGISTRATION)
                .qualificationNumber("1234567890")
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> expertVerifyService.verify(1L, businessRequest(), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EXPERT_PROFILE);

        then(expertProfileRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("NATIONAL_QUALIFICATION 검증 시 파일이 없으면 INVALID_INPUT 예외가 발생한다")
    void verify_국가자격_fileUrl없음_INVALID_INPUT_예외() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> expertVerifyService.verify(1L, nationalRequestWithoutFile(), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("BUSINESS_REGISTRATION 검증 성공 시 verified=true, status=ACTIVE 프로필이 저장된다")
    void verify_사업자검증성공_ACTIVE프로필저장() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(externalVerifyClient.verify(any(), any(boolean.class))).willReturn(true);
        given(expertProfileRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var response = expertVerifyService.verify(1L, businessRequest(), null);

        assertThat(response.verified()).isTrue();
        assertThat(response.status()).isEqualTo(ExpertStatus.ACTIVE);
        then(expertProfileRepository).should().save(any(ExpertProfile.class));
    }

    @Test
    @DisplayName("BUSINESS_REGISTRATION 검증 실패 시 EXPERT_NOT_VERIFIED 예외가 발생한다")
    void verify_사업자검증실패_EXPERT_NOT_VERIFIED_예외() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(externalVerifyClient.verify(any(), any(boolean.class))).willReturn(false);

        assertThatThrownBy(() -> expertVerifyService.verify(1L, businessRequest(), null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXPERT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("NATIONAL_QUALIFICATION 검증 시 보류 상태 프로필이 저장된다")
    void verify_국가자격검증_PENDING_VERIFICATION프로필저장() {
        MultipartFile file = mockFile();
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(externalVerifyClient.verify(any(), any(boolean.class))).willReturn(true);
        given(appealStorageClient.upload(file)).willReturn("expert/cert/uuid.pdf");
        given(expertProfileRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var response = expertVerifyService.verify(1L, nationalRequest(), file);

        assertThat(response.verified()).isFalse();
        assertThat(response.status()).isEqualTo(ExpertStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("국세청 API 장애 시 보류 상태 프로필이 저장된다")
    void verify_국세청API장애_PENDING_VERIFICATION프로필저장() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(expertProfileRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(externalVerifyClient.verify(any(), any(boolean.class)))
                .willThrow(new CustomException(ErrorCode.EXTERNAL_API_FAILURE));
        given(expertProfileRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var response = expertVerifyService.verify(1L, businessRequest(), null);

        assertThat(response.verified()).isFalse();
        assertThat(response.status()).isEqualTo(ExpertStatus.PENDING_VERIFICATION);
    }
}
