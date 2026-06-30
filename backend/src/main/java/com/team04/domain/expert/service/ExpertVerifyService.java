package com.team04.domain.expert.service;

import com.team04.domain.expert.client.ExternalVerifyClient;
import com.team04.domain.expert.dto.request.ExpertVerifyRequest;
import com.team04.domain.expert.dto.response.ExpertVerifyResponse;
import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.domain.expert.repository.ExpertProfileRepository;
import com.team04.domain.user.entity.User;
import com.team04.domain.user.repository.UserRepository;
import com.team04.global.exception.CustomException;
import com.team04.global.exception.ErrorCode;
import com.team04.global.storage.AppealStorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertVerifyService {

    private final ExpertProfileRepository expertProfileRepository;
    private final UserRepository userRepository;
    private final ExternalVerifyClient externalVerifyClient;
    private final AppealStorageClient appealStorageClient;

    @Transactional
    public ExpertVerifyResponse verify(Long userId, ExpertVerifyRequest request, MultipartFile file) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        expertProfileRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isVerified()) {
                throw new CustomException(ErrorCode.DUPLICATE_EXPERT_PROFILE);
            }
            expertProfileRepository.delete(existing);
        });

        // NATIONAL_QUALIFICATION → 파일 필수 검증
        if (request.qualificationType() == QualificationType.NATIONAL_QUALIFICATION
                && (file == null || file.isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 파일 업로드 → 객체 키 저장
        String fileKey = null;
        if (file != null && !file.isEmpty()) {
            fileKey = appealStorageClient.upload(file);
        }

        try {
            boolean verified = externalVerifyClient.verify(request, true);

            if (request.qualificationType() == QualificationType.NATIONAL_QUALIFICATION) {
                ExpertProfile pending = ExpertProfile.ofPending(
                        user,
                        request.qualificationType(),
                        request.qualificationNumber(),
                        fileKey,  // 객체 키 저장
                        null,
                        null
                );
                expertProfileRepository.save(pending);
                log.info("국가자격 검증 보류 처리: userId={}", userId);
                return ExpertVerifyResponse.from(pending);
            }

            if (!verified) {
                throw new CustomException(ErrorCode.EXPERT_NOT_VERIFIED);
            }

            ExpertProfile profile = ExpertProfile.builder()
                    .user(user)
                    .qualificationType(request.qualificationType())
                    .qualificationNumber(request.qualificationNumber())
                    .build();

            expertProfileRepository.save(profile);
            return ExpertVerifyResponse.from(profile);

        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.EXTERNAL_API_FAILURE) {
                log.error("국세청 API 장애, 보류 처리: userId={}", userId);
                ExpertProfile pending = ExpertProfile.ofPending(
                        user,
                        request.qualificationType(),
                        request.qualificationNumber(),
                        null,
                        request.startDate(),
                        request.representativeName()
                );
                expertProfileRepository.save(pending);
                return ExpertVerifyResponse.from(pending);
            }
            throw e;
        }
    }
}