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

        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        if (!profile.isVerified()) {
            throw new CustomException(ErrorCode.EXPERT_NOT_VERIFIED);
        }

        profile.updateProfile(
                request.techStack(),
                request.portfolioUrl(),
                request.career()
        );

        return ExpertProfileResponse.from(profile);
    }


    @Transactional(readOnly = true)
    public ExpertProfileResponse getProfile(Long expertId) {
        ExpertProfile profile = expertProfileRepository.findById(expertId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        return ExpertProfileResponse.from(profile);
    }
}