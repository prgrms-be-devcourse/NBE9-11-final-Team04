package com.team04.domain.user.service;

import com.team04.domain.user.dto.PasswordChangeRequest;
import com.team04.domain.user.dto.UserUpdateRequest;
import com.team04.domain.user.entity.Profile;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.ProfileRepository;
import com.team04.domain.user.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
        User user = mock(User.class);
        Profile profile = mock(Profile.class);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        userService.getMe(1L);

        verify(userRepository).findById(1L);
        verify(profileRepository).findByUserId(1L);
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
    @DisplayName("내 정보 조회 실패 - 프로필 없음")
    void getMe_프로필없음() {
        User user = mock(User.class);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.empty());

        userService.getMe(1L);

        verify(userRepository).findById(1L);
        verify(profileRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("내 정보 수정 성공")
    void updateMe_성공() {
        User user = mock(User.class);
        Profile profile = mock(Profile.class);
        UserUpdateRequest request = new UserUpdateRequest("새닉네임", "소개글", "https://portfolio.com");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.of(profile));

        userService.updateMe(1L, request);

        verify(user).update("새닉네임");
        verify(profile).update("소개글", "https://portfolio.com");
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
    @DisplayName("내 정보 수정 성공 - 프로필 없음")
    void updateMe_프로필없음() {
        User user = mock(User.class);
        UserUpdateRequest request = new UserUpdateRequest("새닉네임", "소개글", "https://portfolio.com");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(profileRepository.findByUserId(1L)).willReturn(Optional.empty());

        userService.updateMe(1L, request);

        verify(user).update("새닉네임");
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_성공() {
        User user = mock(User.class);
        PasswordChangeRequest request = new PasswordChangeRequest("current1!", "newPass1!");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(user.getPassword()).willReturn("encodedCurrent");
        given(passwordEncoder.matches("current1!", "encodedCurrent")).willReturn(true);
        given(passwordEncoder.encode("newPass1!")).willReturn("encodedNew");

        userService.changePassword(1L, request);

        verify(user).changePassword("encodedNew");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void changePassword_현재비밀번호불일치() {
        User user = mock(User.class);
        PasswordChangeRequest request = new PasswordChangeRequest("wrongPass1!", "newPass1!");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(user.getPassword()).willReturn("encodedCurrent");
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
        User user = mock(User.class);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userService.withdraw(1L);

        verify(user).withdraw();
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
