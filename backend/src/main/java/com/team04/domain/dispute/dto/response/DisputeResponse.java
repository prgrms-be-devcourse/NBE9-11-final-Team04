package com.team04.domain.dispute.dto.response;

import com.team04.domain.dispute.entity.Dispute;
import com.team04.domain.dispute.entity.DisputeStatus;

import java.time.LocalDateTime;

public record DisputeResponse(
        Long id,
        Long ideaId,
        Long reporterId,
        Long proposerId,
        DisputeStatus status,
        String reason,
        String evidenceUrl,
        LocalDateTime createdAt
) {
    public static DisputeResponse of(Dispute dispute) {
        return new DisputeResponse(
                dispute.getId(),
                dispute.getIdea().getId(),
                dispute.getReporter().getId(),
                dispute.getProposer().getId(),
                dispute.getStatus(),
                dispute.getReason(),
                dispute.getEvidenceUrl(),
                dispute.getCreatedAt()
        );
    }
}

