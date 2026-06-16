package com.team04.domain.verification.dto.openai;

import com.team04.domain.verification.entity.VerificationCheckCode;
import com.team04.domain.verification.entity.VerificationDecision;

import java.util.List;

/** OpenAI Structured Output으로 수신하는 검증 결과 스키마입니다. */
public record AiVerificationStructuredResult(
        VerificationDecision decision,
        List<CheckResult> checks,
        String reason
) {

    /** 검증 항목별 통과 여부와 점수 및 사유를 표현합니다. */
    public record CheckResult(
            VerificationCheckCode checkCode,
            Boolean passed,
            Integer score,
            String reason
    ) {
    }
}
