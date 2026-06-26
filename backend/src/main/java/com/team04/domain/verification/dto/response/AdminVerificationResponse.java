package com.team04.domain.verification.dto.response;

import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationStatus;

import java.time.LocalDateTime;

/** 관리자 검증 장애 목록 응답 본문입니다. */
public record AdminVerificationResponse(
        Long verificationId,
        Long ideaId,
        VerificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 검증 엔티티로 관리자 목록 응답 객체를 생성합니다. */
    public static AdminVerificationResponse of(ProjectVerification verification) {
        return new AdminVerificationResponse(
                verification.getId(),
                verification.getIdeaId(),
                verification.getStatus(),
                verification.getCreatedAt(),
                verification.getUpdatedAt()
        );
    }
}
