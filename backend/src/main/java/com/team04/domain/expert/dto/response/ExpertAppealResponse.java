package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.AppealStatus;
import com.team04.domain.expert.entity.ExpertAppeal;
import com.team04.global.storage.AppealStorageClient;

import java.time.LocalDateTime;

public record ExpertAppealResponse(
        Long appealId,
        Long expertProfileId,
        String fileUrl,
        String content,
        LocalDateTime submittedAt,
        AppealStatus status,
        int appealCount
) {
    public static ExpertAppealResponse from(
            ExpertAppeal appeal,
            int appealCount,
            AppealStorageClient appealStorageClient
    ) {
        String fileUrl = null;
        if (appeal.getFileUrl() != null) {
            fileUrl = appealStorageClient.getAccessUrl(appeal.getFileUrl());
        }

        return new ExpertAppealResponse(
                appeal.getId(),
                appeal.getExpertProfile().getId(),
                fileUrl,
                appeal.getContent(),
                appeal.getSubmittedAt(),
                appeal.getStatus(),
                appealCount
        );
    }
}
