package com.team04.domain.expert.service;

import com.team04.domain.expert.dto.request.ExpertProfileRequest;
import com.team04.domain.expert.dto.response.ExpertAppealResponse;
import com.team04.domain.expert.dto.response.ExpertProfileListResponse;
import com.team04.domain.expert.dto.response.ExpertProfileResponse;
import com.team04.domain.expert.dto.response.ExpertVerifyResponse;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.TechStack;
import com.team04.domain.expert.repository.ExpertAppealRepository;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.user.entity.Role;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.AppealStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ExpertProfileService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final ExpertAppealRepository expertAppealRepository;
    private final AppealStorageClient appealStorageClient;

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.changeRole(Role.EXPERT);

        return ExpertProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public ExpertProfileResponse getProfile(Long expertId) {
        ExpertProfile profile = expertProfileRepository.findById(expertId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        return ExpertProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public Page<ExpertProfileListResponse> getProfiles(TechStack techStack, Pageable pageable) {
        return expertProfileRepository.findActiveProfiles(techStack, pageable)
                .map(ExpertProfileListResponse::from);
    }

    // 상태별 전문가 목록 조회
    @Transactional(readOnly = true)
    public Page<ExpertProfileResponse> getProfilesByStatus(ExpertStatus status, Pageable pageable) {
        return expertProfileRepository.findProfilesByStatus(status, pageable)
                .map(ExpertProfileResponse::from);
    }

    @Transactional(readOnly = true)
    public ExpertProfileResponse getMyProfile(Long userId) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));
        return ExpertProfileResponse.from(profile);
    }

    @Transactional
    public ExpertProfileResponse updateProfile(Long userId, ExpertProfileRequest request) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));
        profile.updateProfile(request.techStack(), request.portfolioUrl(), request.career());
        return ExpertProfileResponse.from(profile);
    }

    // 소명 자료 목록 조회
    @Transactional(readOnly = true)
    public List<ExpertAppealResponse> getAppeals(Long expertProfileId) {

        // findById → existsById로 변경
        if (!expertProfileRepository.existsById(expertProfileId)) {
            throw new CustomException(ErrorCode.EXPERT_NOT_FOUND);
        }

        return expertAppealRepository
                .findByExpertProfileIdOrderBySubmittedAtDesc(expertProfileId)
                .stream()
                .map(appeal -> ExpertAppealResponse.from(appeal, 0, appealStorageClient))
                .toList();
    }
}