package com.team04.domain.verification.event;

import com.team04.domain.verification.dto.request.VerificationRequest;

/** 트랜잭션 커밋 후 비동기 AI 검증을 시작하기 위한 이벤트입니다. */
public record VerificationRequestedEvent(
        Long verificationId,
        VerificationRequest request
) {
}
