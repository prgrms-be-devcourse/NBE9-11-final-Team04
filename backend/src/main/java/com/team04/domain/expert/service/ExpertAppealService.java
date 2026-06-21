package com.team04.domain.expert.service;

import com.team04.domain.expert.dto.request.ExpertAppealRequest;
import com.team04.domain.expert.dto.response.ExpertAppealResponse;
import com.team04.domain.expert.entity.ExpertAppeal;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.repository.ExpertAppealRepository;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpertAppealService {

    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertAppealRepository expertAppealRepository;
    private final StorageClient storageClient;

    private static final int MAX_APPEAL_COUNT = 3;
    private static final int APPEAL_DEADLINE_DAYS = 7;

    @Transactional
    public ExpertAppealResponse submitAppeal(
            Long userId,
            ExpertAppealRequest request,
            MultipartFile file
    ) {
        ExpertProfile profile = expertProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXPERT_NOT_FOUND));

        // SUSPENDED 상태 확인
        if (profile.getStatus() != ExpertStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.EXPERT_NOT_SUSPENDED);
        }

        // 7일 이내 확인
        if (profile.getSuspendedAt() == null ||
                profile.getSuspendedAt().plusDays(APPEAL_DEADLINE_DAYS).isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.APPEAL_PERIOD_EXPIRED);
        }

        // 3회 제한 확인
        if (profile.getAppealCount() >= MAX_APPEAL_COUNT) {
            throw new CustomException(ErrorCode.APPEAL_LIMIT_EXCEEDED);
        }

        // 파일 업로드
        String fileUrl = null;
        if (file != null && !file.isEmpty()) {
            fileUrl = storageClient.upload(file, "expert/appeal");
        }

        // 소명 자료 저장
        ExpertAppeal appeal = ExpertAppeal.create(profile, fileUrl, request.content());
        expertAppealRepository.save(appeal);

        // 제출 횟수 증가
        profile.increaseAppealCount();

        return ExpertAppealResponse.from(appeal, profile.getAppealCount());
    }
}
