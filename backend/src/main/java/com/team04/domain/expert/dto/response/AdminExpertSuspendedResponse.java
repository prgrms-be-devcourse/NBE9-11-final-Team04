package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.ExpertProfile;
import com.team04.domain.expert.entity.ExpertStatus;
import com.team04.domain.expert.entity.QualificationType;
import com.team04.global.storage.AppealStorageClient;

import java.time.LocalDateTime;

public record AdminExpertSuspendedResponse(
        Long expertProfileId,
        Long userId,
        String name,
        String email,
        QualificationType qualificationType,
        String qualificationNumber,
        ExpertStatus status,
        LocalDateTime suspendedAt,
        int appealCount,
        String fileUrl
) {
    public static AdminExpertSuspendedResponse from(ExpertProfile profile, AppealStorageClient storageClient) {
        String fileUrl = null;
        if (profile.getFileUrl() != null) {
            fileUrl = storageClient.getAccessUrl(profile.getFileUrl());
        }
        return new AdminExpertSuspendedResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getName(),
                profile.getUser().getEmail(),
                profile.getQualificationType(),
                profile.getQualificationNumber(),
                profile.getStatus(),
                profile.getSuspendedAt(),
                profile.getAppealCount(),
                fileUrl
        );
    }
}