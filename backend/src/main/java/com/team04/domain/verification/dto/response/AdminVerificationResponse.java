package com.team04.domain.verification.dto.response;

import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationStatus;

import java.time.LocalDateTime;

public record AdminVerificationResponse(
        Long verificationId,
        Long ideaId,
        VerificationStatus status,
        int resubmissionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminVerificationResponse of(ProjectVerification verification) {
        return new AdminVerificationResponse(
                verification.getId(),
                verification.getIdeaId(),
                verification.getStatus(),
                verification.getResubmissionCount(),
                verification.getCreatedAt(),
                verification.getUpdatedAt()
        );
    }
}
