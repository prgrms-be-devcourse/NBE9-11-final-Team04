package com.team04.infra.openai;

import com.team04.domain.verification.dto.openai.OpenAiVerificationRequest;
import com.team04.domain.verification.dto.openai.OpenAiVerificationResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** 로컬 개발 환경에서 OpenAI API 호출 없이 고정 응답을 반환하는 Mock 클라이언트입니다. */
@Profile("local")
@Primary
@Component
public class MockOpenAiVerificationClient implements OpenAiVerificationClient {

    /** 실제 OpenAI 호출 없이 검증 통과 고정 응답을 반환합니다. */
    @Override
    public OpenAiVerificationResponse verify(OpenAiVerificationRequest request) {
        String mockContent = """
                {
                  "decision": "PASS",
                  "checks": [
                    {"checkCode": "EXAGGERATED_ADVERTISEMENT", "passed": true, "score": 20, "reason": "과대광고 문구 없음"},
                    {"checkCode": "SIMILAR_SERVICE", "passed": true, "score": 20, "reason": "유사 서비스 없음"},
                    {"checkCode": "MILESTONE_SPECIFICITY", "passed": true, "score": 20, "reason": "마일스톤 구체적"}
                  ],
                  "reason": "Mock 검증 통과"
                }
                """;
        return new OpenAiVerificationResponse(
                List.of(new OpenAiVerificationResponse.Choice(
                        new OpenAiVerificationResponse.Message(mockContent)
                ))
        );
    }
}
