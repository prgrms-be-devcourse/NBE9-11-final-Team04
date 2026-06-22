package com.team04.domain.verification.dto.response;

import com.team04.domain.verification.entity.VerificationCheckCode;
import com.team04.domain.verification.entity.VerificationResult;

public record VerificationResultResponse(
        VerificationCheckCode checkCode,
        Boolean passed,
        Integer score,
        String reason
) {
    public static VerificationResultResponse of(VerificationResult result) {
        return new VerificationResultResponse(
                result.getCheckCode(),
                result.getPassed(),
                result.getScore(),
                result.getReason()
        );
    }
}
