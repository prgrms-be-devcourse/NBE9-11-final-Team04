package com.team04.domain.user.service;

import com.team04.domain.user.dto.request.PasswordChangeRequest;
import com.team04.domain.user.dto.request.UserUpdateRequest;
import com.team04.domain.user.entity.Profile;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.ProfileRepository;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.common.Role;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMe_성공() {
        User user = User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);
        Profile profile = Profile.create(user);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        var response = userService.getMe(1L);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.nickname()).isEqualTo("길동이");
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 유저 없음")
    void getMe_유저없음() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 탈퇴한 유저")
    void getMe_탈퇴한유저() {
        User user = User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);
        user.withdraw();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getMe(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN);
    }

    @Test
    @DisplayName("내 정보 조회 성공 - 프로필 없음")
    void getMe_프로필없음() {
        User user = User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.empty());

        var response = userService.getMe(1L);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.intro()).isNull();
    }

    @Test
    @DisplayName("내 정보 수정 성공")
    void updateMe_성공() {
        User user = User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);
        Profile profile = Profile.create(user);
        UserUpdateRequest request = new UserUpdateRequest("새닉네임", "소개글", "https://portfolio.com");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        userService.updateMe(1L, request);

        assertThat(user.getNickname()).isEqualTo("새닉네임");
        assertThat(profile.getIntro()).isEqualTo("소개글");
        assertThat(profile.getPortfolioUrl()).isEqualTo("https://portfolio.com");
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 유저 없음")
    void updateMe_유저없음() {
        UserUpdateRequest request = new UserUpdateRequest("새닉네임", "소개글", "https://portfolio.com");

        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateMe(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 정보 수정 실패 - 탈퇴한 유저")
    void updateMe_탈퇴한유저() {
        User user = User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);
        user.withdraw();
        UserUpdateRequest request = new UserUpdateRequest("새닉네임", "소개글", "https://portfolio.com");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateMe(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWN);
    }

    @Test
    @DisplayName("내 정보 수정 성공 - 프로필 없음")
    void updateMe_프로필없음() {
        User user = User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);
        UserUpdateRequest request = new UserUpdateRequest("새닉네임", "소개글", "https://portfolio.com");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.empty());

        userService.updateMe(1L, request);

        assertThat(user.getNickname()).isEqualTo("새닉네임");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_성공() {
        User user = User.create("test@test.com", "encodedCurrent", "홍길동", "길동이", 25, Role.PROPOSER);
        PasswordChangeRequest request = new PasswordChangeRequest("current1!", "newPass1!");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("current1!", "encodedCurrent")).willReturn(true);
        given(passwordEncoder.encode("newPass1!")).willReturn("encodedNew");

        userService.changePassword(1L, request);

        assertThat(user.getPassword()).isEqualTo("encodedNew");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void changePassword_현재비밀번호불일치() {
        User user = User.create("test@test.com", "encodedCurrent", "홍길동", "길동이", 25, Role.PROPOSER);
        PasswordChangeRequest request = new PasswordChangeRequest("wrongPass1!", "newPass1!");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPass1!", "encodedCurrent")).willReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 유저 없음")
    void changePassword_유저없음() {
        PasswordChangeRequest request = new PasswordChangeRequest("current1!", "newPass1!");

        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_성공() {
        User user = User.create("test@test.com", "encodedPassword", "홍길동", "길동이", 25, Role.PROPOSER);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.withdraw(1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 유저 없음")
    void withdraw_유저없음() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw(1L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
