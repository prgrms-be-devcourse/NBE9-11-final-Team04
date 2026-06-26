package com.team04.domain.dispute.dto.response;

import com.team04.domain.dispute.entity.Dispute;
import com.team04.domain.dispute.entity.DisputeCategory;
import com.team04.domain.dispute.entity.DisputeStatus;
import com.team04.domain.dispute.entity.TargetType;

import java.time.LocalDateTime;

public record AdminDisputeResponse(
        Long id,
        TargetType targetType,
        Long targetId,
        DisputeCategory category,
        String title,
        DisputeStatus status,
        Long reporterId,
        String reporterNickname,
        Long reportedId,
        String reportedNickname,
        LocalDateTime createdAt,
        String ideaStatus
) {
    public static AdminDisputeResponse of(Dispute dispute, String ideaStatus) {
        return new AdminDisputeResponse(
                dispute.getId(),
                dispute.getTargetType(),
                dispute.getTargetId(),
                dispute.getCategory(),
                dispute.getTitle(),
                dispute.getStatus(),
                dispute.getReporter().getId(),
                dispute.getReporter().getNickname(),
                dispute.getReported().getId(),
                dispute.getReported().getNickname(),
                dispute.getCreatedAt(),
                ideaStatus
        );
    }

    public static AdminDisputeResponse of(Dispute dispute) {
        return of(dispute, null);
    }
}
