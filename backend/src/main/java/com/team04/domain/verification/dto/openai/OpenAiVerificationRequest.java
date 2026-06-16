package com.team04.domain.verification.dto.openai;

import java.util.List;
import java.util.Map;

/** OpenAI Structured Output 호출에 전달할 채팅 요청 본문입니다. */
public record OpenAiVerificationRequest(
        String model,
        List<Message> messages,
        ResponseFormat responseFormat
) {

    /** OpenAI 채팅 메시지 한 건을 표현합니다. */
    public record Message(String role, String content) {
    }

    /** Structured Output JSON 스키마 설정을 표현합니다. */
    public record ResponseFormat(String type, JsonSchema jsonSchema) {
    }

    /** Structured Output의 JSON 스키마 이름과 엄격 모드 설정을 표현합니다. */
    public record JsonSchema(String name, boolean strict, Map<String, Object> schema) {
    }
}
