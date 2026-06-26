package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.AppealStatus;
import com.team04.domain.expert.entity.ExpertAppeal;
import com.team04.global.storage.AppealStorageClient;

import java.time.LocalDateTime;

public record AdminExpertAppealSummaryResponse(
        Long appealId,
        String content,
        String fileUrl,
        AppealStatus status,
        LocalDateTime submittedAt
) {
    public static AdminExpertAppealSummaryResponse from(
            ExpertAppeal appeal,
            AppealStorageClient appealStorageClient
    ) {
        String fileUrl = null;
        if (appeal.getFileUrl() != null) {
            fileUrl = appealStorageClient.getAccessUrl(appeal.getFileUrl());
        }

        return new AdminExpertAppealSummaryResponse(
                appeal.getId(),
                appeal.getContent(),
                fileUrl,
                appeal.getStatus(),
                appeal.getSubmittedAt()
        );
    }
}