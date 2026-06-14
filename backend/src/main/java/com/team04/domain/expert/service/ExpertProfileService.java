package com.team04.domain.expert.service;

import com.team04.domain.expert.dto.request.ExpertProfileRequest;
import com.team04.domain.expert.dto.response.ExpertProfileResponse;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ExpertProfileService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExpertProfileResponse registerProfile(Long userId, ExpertProfileRequest request) {

        // 유저 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 중복 등록 방어
        if (expertProfileRepository.existsByUserId(userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        ExpertProfile profile = ExpertProfile.builder()
                .user(user)
                .qualificationType(request.qualificationType())
                .qualificationNumber(request.qualificationNumber())
                .build();

        expertProfileRepository.save(profile);

        return ExpertProfileResponse.from(profile);
    }
}