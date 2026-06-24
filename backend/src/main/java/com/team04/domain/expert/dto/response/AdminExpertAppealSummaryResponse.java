package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.AppealStatus;
import com.team04.domain.expert.entity.ExpertAppeal;

import java.time.LocalDateTime;

public record AdminExpertAppealSummaryResponse(
        Long appealId,
        String content,
        String fileUrl,
        AppealStatus status,
        LocalDateTime submittedAt
) {
    public static AdminExpertAppealSummaryResponse from(ExpertAppeal appeal) {
        return new AdminExpertAppealSummaryResponse(
                appeal.getId(),
                appeal.getContent(),
                appeal.getFileUrl(),
                appeal.getStatus(),
                appeal.getSubmittedAt()
        );
    }
}