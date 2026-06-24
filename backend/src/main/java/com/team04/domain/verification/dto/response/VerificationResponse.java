package com.team04.domain.verification.dto.response;

import com.team04.domain.verification.entity.ProjectVerification;
import com.team04.domain.verification.entity.VerificationStatus;

import java.util.List;

/** 프로젝트 검증 요청 접수와 결과 조회 API의 응답 본문입니다. */
public record VerificationResponse(
        Long verificationId,
        Long ideaId,
        VerificationStatus status,
        List<VerificationResultResponse> results,
        String message
) {

    /** 검증 요청 엔티티와 안내 문구로 응답 객체를 생성합니다. */
    public static VerificationResponse of(ProjectVerification verification, String message) {
        return new VerificationResponse(
                verification.getId(),
                verification.getIdeaId(),
                verification.getStatus(),
                List.of(),
                message
        );
    }

    /** 검증 요청 엔티티, 검증 결과 목록, 안내 문구로 응답 객체를 생성합니다. */
    public static VerificationResponse of(
            ProjectVerification verification,
            List<VerificationResultResponse> results,
            String message
    ) {
        return new VerificationResponse(
                verification.getId(),
                verification.getIdeaId(),
                verification.getStatus(),
                results,
                message
        );
    }
}
