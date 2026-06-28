package com.team04.domain.expert.service;

import com.team04.domain.expert.dto.response.AdminExpertAppealSummaryResponse;
import com.team04.domain.expert.dto.response.AdminExpertSuspendedResponse;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.repository.ExpertAppealRepository;
import com.team04.domain.expert.repository.ExpertProfileRepository;
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
public class AdminExpertService {

    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertAppealRepository expertAppealRepository;
    private final ExpertVerificationService expertVerificationService;
    private final AppealStorageClient appealStorageClient;

    @Transactional(readOnly = true)
    public Page<AdminExpertSuspendedResponse> getExperts(String status, Pageable pageable) {
        if (StringUtils.hasText(status)) {
            ExpertStatus expertStatus;
            try {
                expertStatus = ExpertStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            return expertProfileRepository
                    .findProfilesByStatus(expertStatus, pageable)
                    .map(AdminExpertSuspendedResponse::from);
        }
        return expertProfileRepository
                .findAllProfiles(pageable)
                .map(AdminExpertSuspendedResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AdminExpertSuspendedResponse> getSuspendedExperts(Pageable pageable) {
        return expertProfileRepository
                .findProfilesByStatus(ExpertStatus.SUSPENDED, pageable)
                .map(AdminExpertSuspendedResponse::from);
    }

    @Transactional(readOnly = true)
    public List<AdminExpertAppealSummaryResponse> getAppeals(Long expertProfileId) {
        if (!expertProfileRepository.existsById(expertProfileId)) {
            throw new CustomException(ErrorCode.EXPERT_NOT_FOUND);
        }
        return expertAppealRepository
                .findByExpertProfileIdOrderBySubmittedAtDesc(expertProfileId)
                .stream()
                .map(appeal -> AdminExpertAppealSummaryResponse.from(appeal, appealStorageClient))
                .toList();
    }

    public void restoreExpert(Long expertProfileId) {
        expertVerificationService.restoreProfile(expertProfileId);
    }

    public void demoteExpert(Long expertProfileId) {
        expertVerificationService.demoteToSponsor(expertProfileId);
    }
}