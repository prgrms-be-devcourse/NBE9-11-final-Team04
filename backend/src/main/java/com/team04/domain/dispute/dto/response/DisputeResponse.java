package com.team04.domain.dispute.dto.response;

import com.team04.domain.dispute.entity.AppealStatus;
import com.team04.domain.dispute.entity.Dispute;
import com.team04.domain.dispute.entity.DisputeAppeal;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.entity.TargetType;

import java.time.LocalDateTime;

public record DisputeResponse(
        Long id,
        TargetType targetType,
        Long targetId,
        Long reporterId,
        Long reportedId,
        DisputeCategory category,
        String title,
        DisputeStatus status,
        String reason,
        String evidenceUrl,
        LocalDateTime createdAt,
        AppealInfo appeal
) {
    public record AppealInfo(
            AppealStatus status,
            int appealCount,
            String content,
            String fileUrl,
            LocalDateTime createdAt
    ) {
        public static AppealInfo of(DisputeAppeal appeal) {
            return new AppealInfo(
                    appeal.getStatus(),
                    appeal.getAppealCount(),
                    appeal.getContent(),
                    appeal.getFileUrl(),
                    appeal.getCreatedAt()
            );
        }
    }

    public static DisputeResponse of(Dispute dispute) {
        return new DisputeResponse(
                dispute.getId(),
                dispute.getTargetType(),
                dispute.getTargetId(),
                dispute.getReporter().getId(),
                dispute.getReported().getId(),
                dispute.getCategory(),
                dispute.getTitle(),
                dispute.getStatus(),
                dispute.getReason(),
                dispute.getEvidenceUrl(),
                dispute.getCreatedAt(),
                dispute.getAppeal() != null ? AppealInfo.of(dispute.getAppeal()) : null
        );
    }
}
