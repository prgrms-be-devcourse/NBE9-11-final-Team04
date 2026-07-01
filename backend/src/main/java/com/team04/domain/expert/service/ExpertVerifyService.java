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

        // verified=true인 경우에만 중복 방어
        expertProfileRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isVerified()) {
                throw new CustomException(ErrorCode.DUPLICATE_EXPERT_PROFILE);
            }
            // 미검증 상태면 기존 프로필 삭제 후 재등록
            expertProfileRepository.delete(existing);
        });

        // NATIONAL_QUALIFICATION → 파일 첨부 필수 검증
        if (request.qualificationType() == QualificationType.NATIONAL_QUALIFICATION
                && (file == null || file.isEmpty())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        try {
            boolean verified = externalVerifyClient.verify(request, true);

            if (request.qualificationType() == QualificationType.NATIONAL_QUALIFICATION) {
                // 파일 업로드 후 보류 상태로 저장
                String fileKey = appealStorageClient.upload(file);
                ExpertProfile pending = ExpertProfile.ofPending(
                        user,
                        request.qualificationType(),
                        request.qualificationNumber(),
                        fileKey,
                        null,   // startDate 불필요
                        null    // representativeName 불필요
                );
                expertProfileRepository.save(pending);
                log.info("국가자격 검증 보류 처리: userId={}", userId);
                return ExpertVerifyResponse.from(pending);
            }

            if (!verified) {
                // 국세청 API가 유효하지 않은 사업자로 응답
                throw new CustomException(ErrorCode.EXPERT_NOT_VERIFIED);
            }

            // 국세청 검증 성공
            ExpertProfile profile = ExpertProfile.builder()
                    .user(user)
                    .qualificationType(request.qualificationType())
                    .qualificationNumber(request.qualificationNumber())
                    .build();

            expertProfileRepository.save(profile);
            return ExpertVerifyResponse.from(profile);

        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.EXTERNAL_API_FAILURE) {
                // 국세청 API 장애 → 보류 처리 (파일 있으면 업로드)
                log.error("국세청 API 장애, 보류 처리: userId={}", userId);
                String fileKey = (file != null && !file.isEmpty()) ? appealStorageClient.upload(file) : null;
                ExpertProfile pending = ExpertProfile.ofPending(
                        user,
                        request.qualificationType(),
                        request.qualificationNumber(),
                        fileKey,
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