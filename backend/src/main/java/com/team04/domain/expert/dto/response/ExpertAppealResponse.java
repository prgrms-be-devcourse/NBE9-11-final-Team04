package com.team04.domain.expert.dto.response;

import com.team04.domain.expert.entity.AppealStatus;
import com.team04.domain.expert.entity.ExpertAppeal;

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
    public static ExpertAppealResponse from(ExpertAppeal appeal, int appealCount) {
        return new ExpertAppealResponse(
                appeal.getId(),
                appeal.getExpertProfile().getId(),
                appeal.getFileUrl(),
                appeal.getContent(),
                appeal.getSubmittedAt(),
                appeal.getStatus(),
                appealCount
        );
    }
}
