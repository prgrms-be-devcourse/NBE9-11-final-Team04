package com.team04.domain.verification.dto.response;

import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationStatus;

import java.time.LocalDateTime;

/** 프로젝트 검증 요청 접수와 재제출 API의 응답 본문입니다. */
public record VerificationResponse(
        Long verificationId,
        Long ideaId,
        VerificationStatus status,
        Integer resubmissionCount,
        LocalDateTime revisionDueAt,
        LocalDateTime waitingUntil,
        String message
) {

    /** 검증 요청 엔티티와 안내 문구로 응답 객체를 생성합니다. */
    public static VerificationResponse of(ProjectVerification verification, String message) {
        return new VerificationResponse(
                verification.getId(),
                verification.getIdeaId(),
                verification.getStatus(),
                verification.getResubmissionCount(),
                verification.getRevisionDueAt(),
                verification.getWaitingUntil(),
                message
        );
    }
}
