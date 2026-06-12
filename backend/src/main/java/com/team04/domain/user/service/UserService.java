package com.team04.domain.user.service;

import com.team04.domain.user.dto.PasswordChangeRequest;
import com.team04.domain.user.dto.UserResponse;
import com.team04.domain.user.dto.UserUpdateRequest;
import com.team04.domain.user.entity.Profile;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.ProfileRepository;
import com.team04.domain.user.repository.UserRepository;
import com.team04.domain.user.status.UserStatus;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = getActiveUserOrThrow(userId);
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        return new UserResponse(user, profile);
    }

    //탈퇴,정지된 회원인지 권한 검증
    private User getActiveUserOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        return user;
    }

    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest request){
        User user = getActiveUserOrThrow(userId);

        user.update(request.nickname());

        Profile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> Profile.create(user));

        profile.update(request.intro(), request.portfolioUrl());
        profileRepository.save(profile);

        return new UserResponse(user, profile);
    }

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request){
        User user = getActiveUserOrThrow(userId);
        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())){
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void withdraw(Long userId){
        User user = getActiveUserOrThrow(userId);

        user.withdraw();
    }
}
