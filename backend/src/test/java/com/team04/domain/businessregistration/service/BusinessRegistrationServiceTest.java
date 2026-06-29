package com.team04.domain.businessregistration.service;

import com.team04.domain.businessregistration.client.BusinessVerifyClient;
import com.team04.domain.businessregistration.dto.request.BusinessRegistrationRequest;
import com.team04.domain.businessregistration.dto.response.BusinessRegistrationResponse;
import com.team04.domain.businessregistration.entity.BusinessRegistration;
import com.team04.domain.businessregistration.repository.BusinessRegistrationRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
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
class BusinessRegistrationServiceTest {

    @Mock private BusinessRegistrationRepository businessRegistrationRepository;
    @Mock private BusinessVerifyClient businessVerifyClient;
    @Mock private UserRepository userRepository;
    @Mock private BusinessRegistrationWriter businessRegistrationWriter;

    @InjectMocks
    private BusinessRegistrationService businessRegistrationService;

    private User activeUser() {
        User user = User.create("test@test.com", "pw", "홍길동", "길동이", 25, Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private BusinessRegistration verifiedRegistration(User user) {
        BusinessRegistration br = BusinessRegistration.create(user, "1234567890");
        br.verify();
        return br;
    }

    // ─────────────────────────────────────────────
    // register
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("사업자 등록 성공")
    void register_성공() {
        User user = activeUser();
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest("1234567890", "홍길동", "20200101");
        BusinessRegistrationResponse expected =
                new BusinessRegistrationResponse("1234567890", true, LocalDateTime.now());

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(businessRegistrationRepository.existsByUserId(1L)).willReturn(false);
        given(businessRegistrationRepository.existsByBusinessNumber("1234567890")).willReturn(false);
        given(businessVerifyClient.verify("1234567890", "홍길동", "20200101")).willReturn(true);
        given(businessRegistrationWriter.save(user, "1234567890")).willReturn(expected);

        BusinessRegistrationResponse response = businessRegistrationService.register(1L, request);

        assertThat(response.businessNumber()).isEqualTo("1234567890");
        assertThat(response.verified()).isTrue();
    }

    @Test
    @DisplayName("사업자 등록 실패 - 유저 없음")
    void register_유저없음_예외() {
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest("1234567890", "홍길동", "20200101");

        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> businessRegistrationService.register(99L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사업자 등록 실패 - 정지된 유저")
    void register_정지된유저_예외() {
        User user = activeUser();
        user.suspend();
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest("1234567890", "홍길동", "20200101");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> businessRegistrationService.register(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_SUSPENDED);
    }

    @Test
    @DisplayName("사업자 등록 실패 - 탈퇴한 유저")
    void register_탈퇴한유저_예외() {
        User user = activeUser();
        user.withdraw();
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest("1234567890", "홍길동", "20200101");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> businessRegistrationService.register(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN);
    }

    @Test
    @DisplayName("사업자 등록 실패 - 이미 등록한 유저")
    void register_사용자이미등록_예외() {
        User user = activeUser();
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest("1234567890", "홍길동", "20200101");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(businessRegistrationRepository.existsByUserId(1L)).willReturn(true);

        assertThatThrownBy(() -> businessRegistrationService.register(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("사업자 등록 실패 - 이미 등록된 사업자 번호")
    void register_사업자번호중복_예외() {
        User user = activeUser();
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest("1234567890", "홍길동", "20200101");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(businessRegistrationRepository.existsByUserId(1L)).willReturn(false);
        given(businessRegistrationRepository.existsByBusinessNumber("1234567890")).willReturn(true);

        assertThatThrownBy(() -> businessRegistrationService.register(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("사업자 등록 실패 - 국세청 API 검증 실패")
    void register_외부API검증실패_예외() {
        User user = activeUser();
        BusinessRegistrationRequest request =
                new BusinessRegistrationRequest("1234567890", "홍길동", "20200101");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(businessRegistrationRepository.existsByUserId(1L)).willReturn(false);
        given(businessRegistrationRepository.existsByBusinessNumber("1234567890")).willReturn(false);
        given(businessVerifyClient.verify("1234567890", "홍길동", "20200101")).willReturn(false);

        assertThatThrownBy(() -> businessRegistrationService.register(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_VERIFICATION_FAILED);
    }

    // ─────────────────────────────────────────────
    // getMyBusiness
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("내 사업자 정보 조회 성공")
    void getMyBusiness_성공() {
        User user = activeUser();
        BusinessRegistration br = verifiedRegistration(user);

        given(businessRegistrationRepository.findByUserId(1L)).willReturn(Optional.of(br));

        BusinessRegistrationResponse response = businessRegistrationService.getMyBusiness(1L);

        assertThat(response.businessNumber()).isEqualTo("1234567890");
        assertThat(response.verified()).isTrue();
    }

    @Test
    @DisplayName("내 사업자 정보 조회 실패 - 등록 정보 없음")
    void getMyBusiness_없음_예외() {
        given(businessRegistrationRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> businessRegistrationService.getMyBusiness(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_REGISTRATION_NOT_FOUND);
    }

    // ─────────────────────────────────────────────
    // deleteMyBusiness
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("사업자 정보 삭제 성공")
    void deleteMyBusiness_성공() {
        User user = activeUser();
        BusinessRegistration br = verifiedRegistration(user);

        given(businessRegistrationRepository.findByUserId(1L)).willReturn(Optional.of(br));

        businessRegistrationService.deleteMyBusiness(1L);

        then(businessRegistrationRepository).should().delete(br);
    }

    @Test
    @DisplayName("사업자 정보 삭제 실패 - 등록 정보 없음")
    void deleteMyBusiness_없음_예외() {
        given(businessRegistrationRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> businessRegistrationService.deleteMyBusiness(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BUSINESS_REGISTRATION_NOT_FOUND);
    }
}
